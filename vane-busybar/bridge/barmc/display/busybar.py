"""
Draws cards on a real BUSY Bar through the official busylib SDK, and forwards its controls.

Layout follows busylib's own two-line notification template: the icon centred on the left edge,
the first line anchored top-left at y=0 and the second bottom-left at y=16 in the `small` font.
Long lines scroll on the device itself, so a card is only redrawn when its content changes.
Every draw sends the same three element ids, which replaces them in place: clearing first
would blank the screen between updates. Icons are uploaded once as PNG assets, named by
content hash.
"""

from __future__ import annotations

import asyncio
import hashlib
import logging
import time

from busylib import AsyncBusyBar, types
from busylib.features import ButtonEvent, EncoderEvent, SelectorEvent, input_events

from ..icons import Library, to_png
from ..state import Card
from . import Inputs

log = logging.getLogger(__name__)

APP_NAME = "barmc"
PANEL_WIDTH = 72
FONT: types.DisplayFontName = "small"
TOP_Y, BOTTOM_Y = 0, 16
# Same rule busylib's notifications use: scroll lines longer than ~12 characters per 72px,
# at 1200 px/minute.
SCROLL_THRESHOLD_CHARS = 12
SCROLL_RATE = 1200
# At most one draw per second; a changed card waits at most this long.
MIN_REDRAW_SECONDS = 1.0


def _rgba(color: str) -> str:
    return color.upper() + "FF"


def _line(element_id: str, text: str, x: int, y: int, align: str, color: str) -> types.TextElement:
    room = PANEL_WIDTH - x
    scrolls = len(text) > SCROLL_THRESHOLD_CHARS * room // PANEL_WIDTH
    return types.TextElement(
        id=element_id,
        text=text,
        font=FONT,
        color=_rgba(color),
        x=x,
        y=y,
        align=align,  # type: ignore[arg-type]
        width=room if scrolls else None,
        scroll_rate=SCROLL_RATE if scrolls else None,
    )


def build_elements(card: Card, icon_path: str, icon_width: int = 12) -> list[types.DisplayElement]:
    """
    The draw elements for one card, given the uploaded icon's asset path and width. A 12px icon
    sits 1px in; a 16px player face starts at the edge, and the text moves right to make room.
    """
    icon_x = 1 if icon_width <= 12 else 0
    text_x = icon_x + icon_width + 2
    # An empty second line is still sent, as a space, so it replaces the previous one.
    return [
        types.ImageElement(id="icon", path=icon_path, x=icon_x, y=8, align="mid_left"),
        _line("line1", card.line1, text_x, TOP_Y, "top_left", card.color1),
        _line("line2", card.line2 or " ", text_x, BOTTOM_Y, "bottom_left", card.color2),
    ]


class BusyBarDisplay:
    """Owns the connection to one BUSY Bar."""

    def __init__(self, addr: str, token: str | None, priority: int, library: Library) -> None:
        self._bar = AsyncBusyBar(addr, token=token)
        self._priority = priority
        self._library = library
        self._uploaded: set[str] = set()
        self._last: object = None
        self._last_draw = 0.0
        self._input_task: asyncio.Task[None] | None = None

    async def start(self, inputs: Inputs) -> None:
        version = await self._bar.version()
        log.info("Connected to BUSY Bar, API %s", version.api_semver or "unknown")
        self._input_task = asyncio.create_task(self._read_inputs(inputs))

    async def show(self, card: Card, elapsed: float) -> None:
        # Progress bars are left to the countdown text on the device: redrawing them would
        # mean a request per pixel of progress.
        key = (
            card.icon,
            card.line1,
            card.line2,
            card.color1,
            card.color2,
            self._library.version,
        )
        if key == self._last or time.monotonic() - self._last_draw < MIN_REDRAW_SECONDS:
            return
        icon = self._library.resolve(card.icon)
        path = await self._icon_asset(to_png(icon))
        await self._bar.display_draw(
            types.DisplayElements(
                application_name=APP_NAME,
                priority=self._priority,
                elements=build_elements(card, path, len(icon[0])),
            ),
        )
        self._last = key
        self._last_draw = time.monotonic()

    async def _icon_asset(self, png: bytes) -> str:
        name = hashlib.sha1(png).hexdigest()[:12] + ".png"
        if name not in self._uploaded:
            await self._bar.assets_upload(application_name=APP_NAME, filename=name, data=png)
            self._uploaded.add(name)
        return name

    async def _read_inputs(self, inputs: Inputs) -> None:
        """Forward the Bar's buttons, wheel and selector, reconnecting the status stream if it drops."""
        while True:
            try:
                async for message in self._bar.stream_status_ws():
                    if not isinstance(message, dict):
                        continue
                    for event in input_events(message):
                        if isinstance(event, ButtonEvent) and event.is_press:
                            if event.button == "ok":
                                inputs.ok()
                            elif event.button == "back":
                                inputs.back()
                        elif isinstance(event, EncoderEvent) and event.delta:
                            inputs.wheel(1 if event.delta > 0 else -1)
                        elif isinstance(event, SelectorEvent):
                            inputs.focus(event.position == "busy")
            except asyncio.CancelledError:
                raise
            except Exception as exc:  # noqa: BLE001 - any stream failure means reconnect
                log.warning("BUSY Bar input stream dropped (%s); reconnecting in 5 s", exc)
            await asyncio.sleep(5)

    async def close(self) -> None:
        if self._input_task is not None:
            self._input_task.cancel()
        try:
            await self._bar.display_clear(application_name=APP_NAME)
        except Exception as exc:  # noqa: BLE001 - best effort on shutdown
            log.debug("Could not clear the display: %s", exc)
        await self._bar.aclose()
