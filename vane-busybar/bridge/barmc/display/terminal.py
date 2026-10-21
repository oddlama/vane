"""
A 72x16 preview of the Bar drawn in the terminal with true-colour half blocks.

Uses the same 3x5 font and icons as the web demo. When stdin is a terminal it also reads keys:
o / Enter = OK, x / Backspace = BACK, [ ] or arrow keys = wheel, b = move the selector to or
from BUSY. On Unix keys arrive through the event loop; Windows has no such hook for consoles, so
they are polled with msvcrt there.
"""

from __future__ import annotations

import asyncio
import contextlib
import os
import sys
from collections.abc import Callable, Mapping

if sys.platform == "win32":
    import msvcrt
else:
    import termios
    import tty

from ..icons import Library
from ..sprites import FONT, text_width
from ..state import Card
from . import Inputs

WIDTH, HEIGHT = 72, 16
OFF = (24, 25, 28)


# Seconds between checks for key presses on Windows.
KEY_POLL_SECONDS = 0.05

# What the keys do, by the text a Unix terminal sends for them.
KEYS = {
    "o": "ok",
    "\n": "ok",
    "\r": "ok",
    "x": "back",
    "\x7f": "back",
    "\x08": "back",
    "\x1b": "back",
    "[": "left",
    "\x1b[D": "left",
    "]": "right",
    "\x1b[C": "right",
    "b": "busy",
}

# Windows reports special keys as a prefix and a scan code; these are the arrows, as Unix sends them.
WINDOWS_ARROWS = {"K": "\x1b[D", "M": "\x1b[C"}


def read_windows_key(getwch: Callable[[], str]) -> str:
    """One key press from msvcrt, with arrow keys translated to the text a Unix terminal sends."""
    key = getwch()
    if key in ("\x00", "\xe0"):
        return WINDOWS_ARROWS.get(getwch(), "")
    return key


def _enable_windows_colors() -> None:
    """Turn on escape-code processing, which consoles before Windows Terminal leave off."""
    import ctypes

    kernel32 = ctypes.windll.kernel32  # type: ignore[attr-defined]
    handle = kernel32.GetStdHandle(-11)  # STD_OUTPUT_HANDLE
    mode = ctypes.c_uint32()
    if kernel32.GetConsoleMode(handle, ctypes.byref(mode)):
        kernel32.SetConsoleMode(handle, mode.value | 0x0004)  # ENABLE_VIRTUAL_TERMINAL_PROCESSING


def _rgb(color: str) -> tuple[int, int, int]:
    return int(color[1:3], 16), int(color[3:5], 16), int(color[5:7], 16)


def color_mode(requested: str, env: Mapping[str, str]) -> str:
    """
    `24bit` or `256` for `requested` (`auto`, `24bit` or `256`).

    Terminals announce 24-bit colour inconsistently, so `auto` only falls back for the one common
    terminal known to lack it: Terminal.app before macOS 26, which garbles 24-bit codes.
    """
    if requested != "auto":
        return requested
    if env.get("COLORTERM", "").lower() in ("truecolor", "24bit"):
        return "24bit"
    return "256" if env.get("TERM_PROGRAM") == "Apple_Terminal" else "24bit"


# Channel levels of the 6x6x6 colour cube in the 256-colour palette (indices 16-231).
CUBE_LEVELS = (0, 95, 135, 175, 215, 255)


def to_256(rgb: tuple[int, int, int]) -> int:
    """The nearest of the 240 palette colours every 256-colour terminal draws the same way."""

    def nearest_level(c: int) -> int:
        return min(range(6), key=lambda i: abs(CUBE_LEVELS[i] - c))

    r, g, b = (nearest_level(c) for c in rgb)
    cube = (CUBE_LEVELS[r], CUBE_LEVELS[g], CUBE_LEVELS[b])
    cube_index = 16 + 36 * r + 6 * g + b
    # The grey ramp (232-255) runs from 8 to 238 in steps of 10.
    grey_index = min(23, max(0, round((sum(rgb) / 3 - 8) / 10)))
    grey = (8 + 10 * grey_index,) * 3

    def distance(c: tuple[int, ...]) -> int:
        return sum((a - b) ** 2 for a, b in zip(c, rgb))

    return 232 + grey_index if distance(grey) < distance(cube) else cube_index


def render(card: Card, elapsed: float, library: Library) -> list[list[str | None]]:
    """The card as a 72x16 framebuffer of hex colours, None for off."""
    fb: list[list[str | None]] = [[None] * WIDTH for _ in range(HEIGHT)]

    def put(x: int, y: int, color: str) -> None:
        if 0 <= x < WIDTH and 0 <= y < HEIGHT:
            fb[y][x] = color

    icon = library.resolve(card.icon)
    # 12x12 icons sit 2px in; a 16x16 face fills the height from the left edge.
    ox = 2 if len(icon[0]) <= 12 else 0
    oy = (HEIGHT - len(icon)) // 2
    for y, row in enumerate(icon):
        for x, color in enumerate(row):
            if color:
                put(ox + x, oy + y, color)
    text_x = ox + len(icon[0]) + 2

    def text(s: str, x: int, y: int, color: str) -> None:
        for ch in s.upper():
            glyph = FONT.get(ch, FONT["?"])
            for r in range(5):
                for k in range(3):
                    if glyph[r][k] == "#" and text_x <= x + k < WIDTH:
                        put(x + k, y + r, color)
            x += 4

    def line(s: str, y: int, color: str) -> None:
        tw, room = text_width(s), WIDTH - text_x
        if tw <= room:
            return text(s, text_x, y, color)
        gap = 14
        offset = int(max(0.0, elapsed - 1.1) * 20) % (tw + gap)
        text(s, text_x - offset, y, color)
        text(s, text_x - offset + tw + gap, y, color)

    line(card.line1, 2, card.color1)
    line(card.line2, 9, card.color2)
    if card.bar is not None:
        for x in range(round(max(0.0, min(1.0, card.bar)) * WIDTH)):
            put(x, 15, card.bar_color)
    return fb


def to_ansi(fb: list[list[str | None]], colors: str = "24bit") -> str:
    """Two pixel rows per terminal row: foreground is the top pixel, background the bottom."""
    out = []
    for y in range(0, HEIGHT, 2):
        cells = []
        for x in range(WIDTH):
            top = _rgb(fb[y][x]) if fb[y][x] else OFF
            bottom = _rgb(fb[y + 1][x]) if fb[y + 1][x] else OFF
            if colors == "256":
                cells.append(f"\x1b[38;5;{to_256(top)}m\x1b[48;5;{to_256(bottom)}m▀")
            else:
                cells.append(f"\x1b[38;2;{top[0]};{top[1]};{top[2]}m\x1b[48;2;{bottom[0]};{bottom[1]};{bottom[2]}m▀")
        out.append("".join(cells) + "\x1b[0m")
    return "\n".join(out)


class TerminalDisplay:
    """Redraws the preview in place; prints a new frame per change when output is not a terminal."""

    def __init__(self, library: Library, colors: str = "auto") -> None:
        self._library = library
        self._colors = color_mode(colors, os.environ)
        self._inputs: Inputs | None = None
        self._last = ""
        self._tty_in = sys.stdin.isatty()
        self._tty_out = sys.stdout.isatty()
        self._saved_termios = None
        self._key_task: asyncio.Task[None] | None = None

    async def start(self, inputs: Inputs) -> None:
        self._inputs = inputs
        if self._tty_out:
            if sys.platform == "win32":
                _enable_windows_colors()
            sys.stdout.write("\x1b[?25l\x1b[2J")
        if not self._tty_in:
            return
        if sys.platform == "win32":
            self._key_task = asyncio.create_task(self.poll_keys(msvcrt.kbhit, msvcrt.getwch))
        else:
            fd = sys.stdin.fileno()
            self._saved_termios = termios.tcgetattr(fd)
            tty.setcbreak(fd)
            asyncio.get_running_loop().add_reader(fd, self._on_unix_key)

    async def show(self, card: Card, elapsed: float) -> None:
        frame = to_ansi(render(card, elapsed, self._library), self._colors)
        if frame == self._last:
            return
        self._last = frame
        if self._tty_out:
            keys = "o OK · x BACK · [ ] wheel · b selector BUSY" if self._tty_in else ""
            sys.stdout.write(f"\x1b[H{frame}\n\x1b[2K{keys}\n")
        else:
            sys.stdout.write(f"{frame}\n\n")
        sys.stdout.flush()

    def _on_unix_key(self) -> None:
        self.press(os.read(sys.stdin.fileno(), 16).decode(errors="ignore"))

    async def poll_keys(self, kbhit: Callable[[], bool], getwch: Callable[[], str]) -> None:
        """Hand every key waiting in the Windows console to [press], until cancelled."""
        while True:
            while kbhit():
                self.press(read_windows_key(getwch))
            await asyncio.sleep(KEY_POLL_SECONDS)

    def press(self, key: str) -> None:
        """Apply one key, as the text a Unix terminal sends for it."""
        inputs = self._inputs
        action = KEYS.get(key)
        if inputs is None or action is None:
            return
        if action == "ok":
            inputs.ok()
        elif action == "back":
            inputs.back()
        elif action == "left":
            inputs.wheel(-1)
        elif action == "right":
            inputs.wheel(1)
        elif action == "busy":
            inputs.focus(not inputs.focused())

    async def close(self) -> None:
        if self._key_task is not None:
            self._key_task.cancel()
            with contextlib.suppress(asyncio.CancelledError):
                await self._key_task
            self._key_task = None
        if self._tty_in and self._saved_termios is not None:
            asyncio.get_running_loop().remove_reader(sys.stdin.fileno())
            termios.tcsetattr(sys.stdin.fileno(), termios.TCSADRAIN, self._saved_termios)
        if self._tty_out:
            sys.stdout.write("\x1b[?25h\n")
            sys.stdout.flush()
