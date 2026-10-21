import asyncio
import importlib
import sys
import types

import pytest

from barmc.display import Inputs, terminal
from barmc.display.terminal import TerminalDisplay, read_windows_key
from barmc.icons import Library


def display_with_inputs():
    events = []
    focused = [False]
    inputs = Inputs(
        ok=lambda: events.append("ok"),
        back=lambda: events.append("back"),
        wheel=lambda delta: events.append(("wheel", delta)),
        focus=lambda busy: (
            events.append(("focus", busy)),
            focused.__setitem__(0, busy),
        ),
        focused=lambda: focused[0],
    )
    display = TerminalDisplay(Library())
    display._inputs = inputs
    return display, events


@pytest.mark.parametrize(
    "key, expected",
    [
        ("o", "ok"),
        ("\r", "ok"),
        ("\n", "ok"),
        ("x", "back"),
        ("\x7f", "back"),
        ("\x08", "back"),
        ("\x1b", "back"),
        ("[", ("wheel", -1)),
        ("\x1b[D", ("wheel", -1)),
        ("]", ("wheel", 1)),
        ("\x1b[C", ("wheel", 1)),
        ("b", ("focus", True)),
    ],
)
def test_keys_map_to_bar_controls(key, expected):
    display, events = display_with_inputs()
    display.press(key)
    assert events == [expected]


def test_unknown_keys_do_nothing():
    display, events = display_with_inputs()
    for key in ("q", "", "\x1b[A"):
        display.press(key)
    assert events == []


@pytest.mark.parametrize(
    "codes, expected",
    [
        (["\xe0", "K"], "\x1b[D"),
        (["\x00", "M"], "\x1b[C"),  # arrows, both prefixes
        (["\xe0", "H"], ""),  # up arrow: no Bar control
        (["\r"], "\r"),
        (["\x08"], "\x08"),
        (["b"], "b"),
    ],
)
def test_windows_keys_read_like_unix_ones(codes, expected):
    pending = list(codes)
    assert read_windows_key(lambda: pending.pop(0)) == expected
    assert pending == []


def test_windows_key_polling_handles_everything_waiting():
    display, events = display_with_inputs()
    pending = ["o", "\xe0", "M", "b", "x"]

    async def scenario():
        task = asyncio.create_task(display.poll_keys(lambda: bool(pending), lambda: pending.pop(0)))
        await asyncio.sleep(terminal.KEY_POLL_SECONDS * 2)
        task.cancel()
        with pytest.raises(asyncio.CancelledError):
            await task

    asyncio.run(scenario())
    assert events == ["ok", ("wheel", 1), ("focus", True), "back"]


def test_imports_on_windows_without_termios(monkeypatch):
    """The module must not need termios or tty, which Windows lacks."""
    monkeypatch.setattr(sys, "platform", "win32")
    monkeypatch.setitem(
        sys.modules,
        "msvcrt",
        types.SimpleNamespace(kbhit=lambda: False, getwch=lambda: ""),
    )
    monkeypatch.setitem(sys.modules, "termios", None)  # importing it now raises ImportError
    monkeypatch.setitem(sys.modules, "tty", None)
    try:
        # Any import of termios or tty would raise here.
        windows = importlib.reload(terminal)
        assert windows.msvcrt is sys.modules["msvcrt"]
    finally:
        monkeypatch.undo()
        importlib.reload(terminal)


@pytest.mark.parametrize(
    "rgb, index",
    [
        ((0, 0, 0), 16),
        ((255, 255, 255), 231),
        ((255, 0, 0), 196),
        ((0, 255, 0), 46),
        ((0, 0, 255), 21),
        ((128, 128, 128), 244),
        ((8, 8, 8), 232),
        ((238, 238, 238), 255),
        ((0x6F, 0xE0, 0x6B), 77),  # the bridge's GREEN: cube levels 95, 215, 95
    ],
)
def test_converts_colours_to_the_256_palette(rgb, index):
    assert terminal.to_256(rgb) == index


@pytest.mark.parametrize(
    "requested, env, mode",
    [
        ("auto", {"TERM_PROGRAM": "Apple_Terminal"}, "256"),
        ("auto", {"TERM_PROGRAM": "Apple_Terminal", "COLORTERM": "truecolor"}, "24bit"),
        ("auto", {"TERM_PROGRAM": "iTerm.app", "COLORTERM": "truecolor"}, "24bit"),
        ("auto", {}, "24bit"),
        ("256", {"COLORTERM": "truecolor"}, "256"),
        ("24bit", {"TERM_PROGRAM": "Apple_Terminal"}, "24bit"),
    ],
)
def test_picks_colours_for_the_terminal(requested, env, mode):
    assert terminal.color_mode(requested, env) == mode


def test_draws_with_palette_codes_in_256_colour_mode():
    from barmc.state import Card

    fb = terminal.render(Card("sun", "HI"), 0.0, Library())
    assert "\x1b[38;2;" in terminal.to_ansi(fb, "24bit")
    palette = terminal.to_ansi(fb, "256")
    assert "\x1b[38;5;" in palette and "\x1b[38;2;" not in palette
