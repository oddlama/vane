import asyncio
import base64
import hashlib
import ssl

import pytest

from barmc.pairing import (
    PinMismatch,
    is_local,
    parse_pairing,
    parse_pin,
    subject_public_key_info,
)
from barmc.server import PluginClient

# Self-signed P-256 test certificate, like the plugin's Tls auto one. Test data only.
CERT = """-----BEGIN CERTIFICATE-----
MIIBhjCCASugAwIBAgIUCfH1wioe9L7TB5lnYODew5WGYbcwCgYIKoZIzj0EAwIw
FzEVMBMGA1UEAwwMdmFuZS1idXN5YmFyMCAXDTI2MTAwMTIxMTUwNFoYDzIxMjYw
OTA3MjExNTA0WjAXMRUwEwYDVQQDDAx2YW5lLWJ1c3liYXIwWTATBgcqhkjOPQIB
BggqhkjOPQMBBwNCAATwFj4Nk4SGT6hYoCa7KREWG5Vp6kB0+e9KBzaDk245EI0S
58XLZtcPkKyqjwmcvCtosaROEZjXrzo0eP3k/sXwo1MwUTAdBgNVHQ4EFgQUaeOf
StHYt4jppKiMKEPSrNormhwwHwYDVR0jBBgwFoAUaeOfStHYt4jppKiMKEPSrNor
mhwwDwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAgNJADBGAiEAjqKba2d/1/ro
BlHFw+sqv8T8NPBvCNxzoxIXbVYLLVYCIQCB+8goi0QrRT2zfIUuD5g4eXzB9tlF
cMO/PfpJbUJzSg==
-----END CERTIFICATE-----
"""
KEY = """-----BEGIN PRIVATE KEY-----
MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgt3PbjIOewuV6meKZ
MQ6ty8PNze+KPb0TjVrUM+GlWPGhRANCAATwFj4Nk4SGT6hYoCa7KREWG5Vp6kB0
+e9KBzaDk245EI0S58XLZtcPkKyqjwmcvCtosaROEZjXrzo0eP3k/sXw
-----END PRIVATE KEY-----
"""
# openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | sha256 | base64url
PIN = "dU3z7bmUsaLgCAeq1YmPX-IIP30zWRWO0X0v2RuhKQ8"
OTHER_PIN = base64.urlsafe_b64encode(hashlib.sha256(b"another key").digest()).decode().rstrip("=")


def test_parses_a_pairing_string():
    pairing = parse_pairing(f"  tok_en-1@203.0.113.7:9123#sha256={PIN}\n")
    assert pairing.server == "https://203.0.113.7:9123"
    assert pairing.token == "tok_en-1"
    assert pairing.pin == parse_pin(f"sha256={PIN}")
    assert len(pairing.pin) == 32


def test_implies_https_but_keeps_an_explicit_scheme():
    assert parse_pairing("tok@localhost:9123").server == "https://localhost:9123"
    assert parse_pairing("https://tok@localhost:9123").server == "https://localhost:9123"
    assert parse_pairing("http://tok@192.168.1.5:9123").server == "http://192.168.1.5:9123"


def test_keeps_the_path_and_bracketed_ipv6_hosts():
    pairing = parse_pairing("https://tok@[2001:db8::1]:9123/busybar")
    assert pairing.server == "https://[2001:db8::1]:9123/busybar"
    assert pairing.pin is None


@pytest.mark.parametrize(
    "text, reason",
    [
        ("203.0.113.7:9123#sha256=" + PIN, "no token"),
        ("ftp://tok@host", "the line from /busybar link"),
        ("http://tok@host:9123#sha256=" + PIN, "only makes sense with https"),
        ("https://tok@host#md5=abc", "unknown key fingerprint"),
        ("https://tok@host#sha256=" + PIN[:-4], "wrong length"),
        ("https://tok@host#sha256=" + PIN[:-1] + "!", "damaged"),
    ],
)
def test_rejects_broken_pairing_strings(text, reason):
    with pytest.raises(ValueError, match=reason):
        parse_pairing(text)


def test_extracts_the_key_the_pin_hashes():
    der = ssl.PEM_cert_to_DER_cert(CERT)
    digest = hashlib.sha256(subject_public_key_info(der)).digest()
    assert digest == parse_pin(f"sha256={PIN}")


@pytest.mark.parametrize(
    "url, local",
    [
        ("http://localhost:9123", True),
        ("http://127.0.0.1:9123", True),
        ("http://192.168.1.5:9123", True),
        ("http://[::1]:9123", True),
        ("http://1.1.1.1:9123", False),
        ("http://mc.example.com:9123", False),
    ],
)
def test_tells_local_addresses_apart(url, local):
    assert is_local(url) is local


async def _serve(tmp_path, received: list[bytes]):
    """A TLS server with the test key, answering one snapshot event and recording what it was sent."""
    (tmp_path / "cert.pem").write_text(CERT)
    (tmp_path / "key.pem").write_text(KEY)
    context = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
    context.load_cert_chain(tmp_path / "cert.pem", tmp_path / "key.pem")

    async def handle(reader, writer):
        try:
            received.append(await reader.readuntil(b"\r\n\r\n"))
            writer.write(
                b"HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nConnection: close\r\n\r\n"
                b'event: snapshot\ndata: {"online": 1}\n\n'
            )
            await writer.drain()
        except (asyncio.IncompleteReadError, ConnectionError, ssl.SSLError):
            pass
        finally:
            writer.close()

    server = await asyncio.start_server(handle, "127.0.0.1", 0, ssl=context)
    return server, f"https://127.0.0.1:{server.sockets[0].getsockname()[1]}"


def test_connects_to_the_pinned_key(tmp_path):
    async def scenario():
        received: list[bytes] = []
        server, url = await _serve(tmp_path, received)
        client = PluginClient(url, "secret-token", parse_pin(f"sha256={PIN}"))
        try:
            events = client.events()
            event = await anext(events)
            await events.aclose()
        finally:
            await client.aclose()
            server.close()
        return event, received

    event, received = asyncio.run(scenario())
    assert (event.kind, event.data) == ("snapshot", {"online": 1})
    assert b"Authorization: Bearer secret-token" in received[0]


def test_never_sends_the_token_to_another_key(tmp_path):
    async def scenario():
        received: list[bytes] = []
        server, url = await _serve(tmp_path, received)
        client = PluginClient(url, "secret-token", parse_pin(f"sha256={OTHER_PIN}"))
        try:
            with pytest.raises(PinMismatch):
                await anext(client.events())
            assert await client.icon() is None  # other requests fail the same way
            await asyncio.sleep(0.1)
        finally:
            await client.aclose()
            server.close()
        return received

    assert not any(b"secret-token" in request for request in asyncio.run(scenario()))


def test_rejects_the_self_signed_key_without_a_pin(tmp_path):
    async def scenario():
        received: list[bytes] = []
        server, url = await _serve(tmp_path, received)
        client = PluginClient(url, "secret-token")
        try:
            assert await client.icon() is None
            await asyncio.sleep(0.1)
        finally:
            await client.aclose()
            server.close()
        return received

    assert asyncio.run(scenario()) == []
