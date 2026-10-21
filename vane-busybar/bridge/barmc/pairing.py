"""
Pairing strings from `/busybar link`, and pinning of the plugin's self-signed TLS key.

A pairing string is the stream URL with the token as user info and, when the plugin runs with
`Tls: auto`, the SHA-256 of its certificate's public key as fragment. `https://` is implied; only a
server with TLS off says `http://`:

    <token>@203.0.113.7:9123#sha256=<base64url>

The fingerprint replaces certificate authorities: the bridge accepts the server only if its key
hashes to it. It arrived through Minecraft's encrypted connection, so nobody in between can
swap in a certificate of their own.
"""

from __future__ import annotations

import base64
import binascii
import hashlib
import ipaddress
import ssl
from dataclasses import dataclass
from urllib.parse import unquote, urlsplit, urlunsplit

PIN_PREFIX = "sha256="


@dataclass(frozen=True)
class Pairing:
    """Where and how to connect, split out of a pairing string."""

    server: str
    """Stream base URL, without token or fingerprint."""
    token: str
    """Bearer token, sent in the Authorization header."""
    pin: bytes | None
    """SHA-256 of the server key's SubjectPublicKeyInfo, or None to validate the certificate normally."""


class PinMismatch(ssl.SSLError):
    """The server's TLS key is not the one in the pairing string."""

    def __str__(self) -> str:
        return "the server's TLS key does not match the pairing string"


def parse_pairing(text: str) -> Pairing:
    """Split the line shown by `/busybar link`. Raises ValueError with a reason players can act on."""
    text = text.strip()
    if "://" not in text:
        text = "https://" + text
    parts = urlsplit(text)
    if parts.scheme not in ("http", "https"):
        raise ValueError("expected the line from /busybar link")
    userinfo, at, hostport = parts.netloc.rpartition("@")
    if not at or not userinfo:
        raise ValueError("it holds no token; copy the whole line from /busybar link")
    pin = parse_pin(parts.fragment) if parts.fragment else None
    if pin is not None and parts.scheme != "https":
        raise ValueError("a key fingerprint only makes sense with https://")
    server = urlunsplit((parts.scheme, hostport, parts.path, "", ""))
    return Pairing(server=server, token=unquote(userinfo), pin=pin)


def parse_pin(value: str) -> bytes:
    """Decode `sha256=<base64url>` to the 32-byte digest."""
    if not value.startswith(PIN_PREFIX):
        raise ValueError(f"unknown key fingerprint {value!r}, expected {PIN_PREFIX}<base64url>")
    encoded = value[len(PIN_PREFIX) :]
    try:
        digest = base64.b64decode(
            encoded.replace("-", "+").replace("_", "/") + "=" * (-len(encoded) % 4),
            validate=True,
        )
    except (binascii.Error, ValueError):
        raise ValueError("the key fingerprint is damaged; copy the whole line from /busybar link") from None
    if len(digest) != hashlib.sha256().digest_size:
        raise ValueError("the key fingerprint has the wrong length; copy the whole line from /busybar link")
    return digest


def is_local(url: str) -> bool:
    """Whether `url` points at this machine or a private network, where plain HTTP is tolerable."""
    host = urlsplit(url).hostname or ""
    if host == "localhost":
        return True
    try:
        address = ipaddress.ip_address(host)
    except ValueError:
        return False
    return address.is_loopback or address.is_private


def pinned_context(pin: bytes) -> ssl.SSLContext:
    """
    A client TLS context that accepts exactly the server key hashing to `pin`.

    Names, issuer and expiry are not checked: the plugin's certificate is self-signed and never
    expires, and the key is what identifies the server. The check runs at the end of the
    handshake, before any request, so the token never reaches a server with another key.
    Only for asyncio clients such as `httpx.AsyncClient`, which use `SSLObject`.
    """
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
    context.minimum_version = ssl.TLSVersion.TLSv1_2
    context.check_hostname = False
    context.verify_mode = ssl.CERT_NONE

    class PinnedSSLObject(ssl.SSLObject):
        def do_handshake(self) -> None:
            super().do_handshake()
            der = self.getpeercert(binary_form=True)
            if der is None or hashlib.sha256(subject_public_key_info(der)).digest() != pin:
                raise PinMismatch()

    context.sslobject_class = PinnedSSLObject
    return context


def subject_public_key_info(der: bytes) -> bytes:
    """The DER SubjectPublicKeyInfo of an X.509 certificate, the part the pin hashes."""
    _, pos, _ = _element(der, 0)  # Certificate
    _, pos, _ = _element(der, pos)  # TBSCertificate
    tag, _, end = _element(der, pos)
    if tag == 0xA0:  # explicit version
        pos = end
    for _ in range(5):  # serialNumber, signature, issuer, validity, subject
        pos = _element(der, pos)[2]
    tag, _, end = _element(der, pos)
    if tag != 0x30:
        raise ValueError("certificate has no SubjectPublicKeyInfo")
    return der[pos:end]


def _element(der: bytes, pos: int) -> tuple[int, int, int]:
    """Tag, content start and end of the DER element at `pos`."""
    tag, length = der[pos], der[pos + 1]
    pos += 2
    if length & 0x80:
        count = length & 0x7F
        length = int.from_bytes(der[pos : pos + count], "big")
        pos += count
    if pos + length > len(der):
        raise ValueError("truncated certificate")
    return tag, pos, pos + length
