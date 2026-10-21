"""
Run the bridge: `barmc --pair PAIRING (--bar ADDRESS | --terminal)`.

Every option can also come from the environment: BARMC_PAIR, BARMC_SERVER, BARMC_TOKEN,
BUSYBAR_ADDR, BUSYBAR_TOKEN. Prefer the environment for the pairing string and tokens, since
command lines are visible to every user of the machine.
"""

from __future__ import annotations

import argparse
import asyncio
import contextlib
import logging
import os
import signal
import sys
import time
from typing import Any

from .display import Display, Inputs
from .icons import Library, face_grid, shrink_server_icon
from .pairing import Pairing, PinMismatch, is_local, parse_pairing
from .server import PluginClient, TokenRejected
from .state import BarState, Card

log = logging.getLogger("barmc")

FRAME_SECONDS = 0.1
DISPLAY_RETRY_SECONDS = 5.0


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(prog="barmc", description=__doc__.split("\n\n")[0].strip())
    parser.add_argument(
        "--pair",
        default=os.environ.get("BARMC_PAIR"),
        help="the whole line shown by /busybar link: server, token and key fingerprint",
    )
    parser.add_argument(
        "--server",
        default=os.environ.get("BARMC_SERVER"),
        help="server URL, instead of --pair; for servers with Tls keystore or off",
    )
    parser.add_argument(
        "--token",
        default=os.environ.get("BARMC_TOKEN"),
        help="token, instead of --pair",
    )
    output = parser.add_mutually_exclusive_group()
    output.add_argument(
        "--bar",
        default=os.environ.get("BUSYBAR_ADDR"),
        help="BUSY Bar address: 10.0.4.20 over USB, or its IP on Wi-Fi",
    )
    output.add_argument(
        "--terminal",
        action="store_true",
        help="preview the display in this terminal instead",
    )
    parser.add_argument(
        "--colors",
        choices=("auto", "24bit", "256"),
        default="auto",
        help="colours for --terminal; auto uses 256 in macOS Terminal.app, 24-bit elsewhere",
    )
    parser.add_argument(
        "--bar-token",
        default=os.environ.get("BUSYBAR_TOKEN"),
        help="the Bar's API password, needed over Wi-Fi",
    )
    parser.add_argument(
        "--priority",
        type=int,
        default=50,
        help="draw priority on the Bar, 1-100 (default 50)",
    )
    parser.add_argument("-v", "--verbose", action="store_true", help="log every event")
    args = parser.parse_args(argv)
    if args.pair:
        try:
            args.pairing = parse_pairing(args.pair)
        except ValueError as exc:
            parser.error(f"--pair: {exc}")
    elif args.server and args.token:
        args.pairing = Pairing(server=args.server, token=args.token, pin=None)
    else:
        parser.error("--pair is required (or BARMC_PAIR), or both --server and --token")
    if not args.terminal and not args.bar:
        parser.error("choose an output: --bar ADDRESS or --terminal")
    return args


def make_display(args: argparse.Namespace, library: Library) -> Display:
    if args.terminal:
        from .display.terminal import TerminalDisplay

        return TerminalDisplay(library, args.colors)
    from .display.busybar import BusyBarDisplay

    return BusyBarDisplay(args.bar, args.bar_token, args.priority, library)


async def run(args: argparse.Namespace) -> int:
    pairing: Pairing = args.pairing
    if pairing.server.startswith("http://") and not is_local(pairing.server):
        log.warning(
            "Connecting without TLS: the token and every event cross the internet unencrypted. "
            "Ask the server admin to set Tls to auto in the vane-busybar config."
        )
    plugin = PluginClient(pairing.server, pairing.token, pairing.pin)
    library = Library()
    display = make_display(args, library)
    background: set[asyncio.Task[Any]] = set()
    icon_changed = asyncio.Event()

    def spawn(coro: Any) -> None:
        task = asyncio.create_task(coro)
        background.add(task)
        task.add_done_callback(background.discard)

    state = BarState(
        send=lambda request: spawn(plugin.send(request)),
        on_icon_changed=lambda tag: icon_changed.set(),
    )
    now = time.monotonic
    inputs = Inputs(
        ok=lambda: state.press_ok(now()),
        back=lambda: state.press_back(now()),
        wheel=lambda delta: state.turn_wheel(delta, now()),
        focus=lambda busy: state.set_focus(busy, now()),
        focused=lambda: state.m.busy,
    )

    fetching: set[str] = set()

    async def fetch_face(skin: str) -> None:
        png = await plugin.head(skin)
        if png:
            library.add_face(skin, face_grid(png))
        fetching.discard(skin)

    async def consume() -> None:
        async for event in plugin.events():
            log.debug("%s %s", event.kind, event.data)
            state.handle(event.kind, event.data, now())
            # The alert shows the generic head until the face arrives, then redraws. Players
            # folded into a burst card never get their own alert, so their faces are skipped.
            skin = event.data.get("head")
            if isinstance(skin, str) and skin not in library.faces and skin not in fetching and state.needs_face(skin):
                fetching.add(skin)
                spawn(fetch_face(skin))

    async def refresh_icon() -> None:
        while True:
            await icon_changed.wait()
            icon_changed.clear()
            png = await plugin.icon() if state.m.icon_tag else None
            library.set_server_icon(shrink_server_icon(png) if png else None)

    async def render() -> None:
        shown: Card | None = None
        since = now()
        while True:
            card, _ = state.frame(now())
            if card != shown:
                shown, since = card, now()
            try:
                await display.show(card, now() - since)
            except Exception as exc:  # noqa: BLE001 - keep running while the Bar is unreachable
                log.warning(
                    "Could not update the display (%s); retrying in %.0f s",
                    exc,
                    DISPLAY_RETRY_SECONDS,
                )
                await asyncio.sleep(DISPLAY_RETRY_SECONDS)
            await asyncio.sleep(FRAME_SECONDS)

    await display.start(inputs)
    tasks = [asyncio.create_task(t) for t in (consume(), refresh_icon(), render())]
    stop = asyncio.Event()
    loop = asyncio.get_running_loop()
    for sig in (signal.SIGINT, signal.SIGTERM):
        with contextlib.suppress(NotImplementedError):
            loop.add_signal_handler(sig, stop.set)
    stopper = asyncio.create_task(stop.wait())
    try:
        done, _ = await asyncio.wait([*tasks, stopper], return_when=asyncio.FIRST_COMPLETED)
        for task in done:
            if task is not stopper and task.exception() is not None:
                raise task.exception()  # type: ignore[misc]
        return 0
    except TokenRejected:
        log.error("The server rejected the token. Run /busybar link in game and use the new pairing string.")
        return 2
    except PinMismatch:
        log.error(
            "The server's TLS key does not match the pairing string, so nothing was sent to it. If an admin "
            "ran /busybar rotatekey, run /busybar link in game and use the new pairing string. Otherwise "
            "someone may be intercepting the connection."
        )
        return 3
    finally:
        for task in [*tasks, stopper, *background]:
            task.cancel()
        await display.close()
        await plugin.aclose()


def main(argv: list[str] | None = None) -> None:
    args = parse_args(argv)
    # The terminal preview owns stdout; keep routine logs out of its way unless asked.
    level = logging.DEBUG if args.verbose else logging.WARNING if args.terminal else logging.INFO
    logging.basicConfig(
        level=level,
        format="%(asctime)s %(levelname)s %(name)s: %(message)s",
        stream=sys.stderr,
    )
    for noisy in ("httpx", "httpx2", "httpcore", "httpcore2", "busylib", "websockets"):
        logging.getLogger(noisy).setLevel(max(level, logging.WARNING))
    # The bottom line is anchored to the panel edge at y=16 on purpose, as in busylib's own
    # two-line notifications; its bounds check warns about that on every draw.
    logging.getLogger("busylib.client.display").addFilter(
        lambda record: "exceeds front height" not in record.getMessage()
    )
    try:
        sys.exit(asyncio.run(run(args)))
    except KeyboardInterrupt:
        # Windows has no signal handlers in asyncio, so Ctrl+C arrives here once run() cleaned up.
        sys.exit(130)


if __name__ == "__main__":
    main()
