"""Client for the vane-busybar plugin: the event stream, requests and the server icon."""

from __future__ import annotations

import asyncio
import json
import logging
import random
from collections.abc import AsyncIterator
from dataclasses import dataclass
from typing import Any

import httpx

from .pairing import PinMismatch, pinned_context

log = logging.getLogger(__name__)


class TokenRejected(Exception):
    """The plugin refused the token; it was revoked or replaced by a newer `/busybar link`."""


@dataclass
class Event:
    """One server-sent event."""

    kind: str
    data: dict[str, Any]


async def parse_sse(lines: AsyncIterator[str]) -> AsyncIterator[Event]:
    """Turn server-sent-event lines into events. Comments (`: ping`) are skipped."""
    kind, data = "message", []
    async for line in lines:
        if line == "":
            if data:
                try:
                    yield Event(kind, json.loads("\n".join(data)))
                except json.JSONDecodeError:
                    log.warning("Ignoring event %s with invalid JSON", kind)
            kind, data = "message", []
        elif line.startswith(":"):
            continue
        else:
            name, _, value = line.partition(":")
            value = value[1:] if value.startswith(" ") else value
            if name == "event":
                kind = value
            elif name == "data":
                data.append(value)


class PluginClient:
    """
    Talks to vane-busybar at `base_url` with the token from `/busybar link`.

    With `pin`, the server must present the TLS key it hashes to (see `pairing`); without, an
    https server needs a certificate the system trusts.

    `events()` reconnects on its own with backoff; it only gives up when the token is rejected or
    the server's key does not match `pin`.
    """

    # The plugin sends a keep-alive every 15 s, so a silent minute means the connection is dead.
    READ_TIMEOUT = 45.0

    def __init__(self, base_url: str, token: str, pin: bytes | None = None) -> None:
        self.base_url = base_url.rstrip("/")
        self.pinned = pin is not None
        self._client = httpx.AsyncClient(
            base_url=self.base_url,
            headers={"Authorization": f"Bearer {token}"},
            timeout=httpx.Timeout(10.0, read=self.READ_TIMEOUT),
            verify=pinned_context(pin) if pin is not None else True,
        )

    async def aclose(self) -> None:
        await self._client.aclose()

    async def events(self) -> AsyncIterator[Event]:
        """Every event from the stream, across reconnects. Each connection starts with `snapshot`."""
        delay = 1.0
        while True:
            try:
                async with self._client.stream("GET", "/events", headers={"Accept": "text/event-stream"}) as response:
                    if response.status_code == 401:
                        raise TokenRejected()
                    response.raise_for_status()
                    log.info(
                        "Connected to %s%s",
                        self.base_url,
                        " (TLS key pinned)" if self.pinned else "",
                    )
                    delay = 1.0
                    async for event in parse_sse(response.aiter_lines()):
                        yield event
            except TokenRejected:
                raise
            except (httpx.HTTPError, OSError) as exc:
                if (mismatch := _cause(exc, PinMismatch)) is not None:
                    raise mismatch from None
                failure = exc.__class__.__name__
            else:
                failure = "stream ended"
            # Jitter, so bridges that lost the server together do not all return in the same second.
            wait = delay * (0.5 + random.random())
            log.warning("Event stream unavailable (%s); retrying in %.1f s", failure, wait)
            await asyncio.sleep(wait)
            delay = min(delay * 2, 15.0)

    async def send(self, request: dict[str, Any]) -> None:
        """Send a request such as `{"type": "presence.set", "busy": true}`."""
        try:
            response = await self._client.post("/action", json=request)
        except httpx.HTTPError as exc:
            log.warning("Request %s failed: %s", request, exc)
            return
        if response.status_code == 403:
            log.warning("Request %s refused: not allowed for this player", request)
        elif response.status_code != 202:
            log.warning("Request %s answered %s", request, response.status_code)

    async def head(self, skin: str) -> bytes | None:
        """A player's 8x8 face PNG for the skin id from a join or quit event, or None."""
        try:
            response = await self._client.get(f"/head/{skin}")
        except httpx.HTTPError as exc:
            log.warning("Could not fetch face %s: %s", skin, exc)
            return None
        if response.status_code == 200:
            return response.content
        log.warning("Face %s answered %s", skin, response.status_code)
        return None

    async def icon(self, etag: str | None = None) -> bytes | None:
        """The server icon PNG, or None if the server has none or it did not change since `etag`."""
        headers = {"If-None-Match": etag} if etag else {}
        try:
            response = await self._client.get("/icon", headers=headers)
        except httpx.HTTPError as exc:
            log.warning("Could not fetch the server icon: %s", exc)
            return None
        if response.status_code == 200:
            return response.content
        if response.status_code not in (304, 404):
            log.warning("Server icon request answered %s", response.status_code)
        return None


def _cause(exc: BaseException, kind: type[BaseException]) -> BaseException | None:
    """The first exception of `kind` in the chain that led to `exc`."""
    seen: BaseException | None = exc
    while seen is not None:
        if isinstance(seen, kind):
            return seen
        seen = seen.__cause__ or seen.__context__
    return None
