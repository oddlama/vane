"""
What the Bar should show, derived from vane-busybar events and the Bar's own inputs.

This is the display-independent part of the bridge: it keeps a mirror of the server state,
queues alerts by priority, rotates the idle screens, and turns button presses into requests
for the server. Renderers only ever see a `Card`.
"""

from __future__ import annotations

import math
from collections.abc import Callable
from dataclasses import dataclass, field
from typing import Any

from .icons import head_icon
from .sprites import AMBER, CYAN, GREEN, GREY, PURPLE, RED, WHITE

ROTATE_SECONDS = 5.0
FOCUS_SECONDS = 25 * 60

# Priorities: 1 info, 2 notice, 3 important, 4 critical. Focus mode only lets 4 through.
CRITICAL = 4

# More alerts of one kind than this within the window become one counting card, so a proxy
# restart, an /advancement grant or a raid does not replay every event for minutes.
BURST_WINDOW = 5.0
BURST_THRESHOLD = 3
ALERT_SECONDS = 3.8
# Non-critical alerts beyond this many in the queue are dropped, lowest priority first, and
# alerts that waited longer than MAX_WAIT_SECONDS are skipped as stale. A "+N MORE" card says so.
MAX_QUEUE = 8
MAX_WAIT_SECONDS = 30.0


@dataclass(frozen=True)
class Card:
    """
    One screen: a 12x12 icon and two lines of text, with an optional progress bar along the bottom.

    `icon` is a sprite name (`bed`, `shield-denied`, ...), `server` / `server-grey` for the
    server icon, or `head:<player>:<skin id>` for a player head.
    """

    icon: str
    line1: str
    line2: str = ""
    color1: str = WHITE
    color2: str = WHITE
    bar: float | None = None
    bar_color: str = WHITE


@dataclass
class Alert:
    """A transient card with a priority and a lifetime."""

    kind: str
    card: Card
    priority: int = 1
    duration: float = ALERT_SECONDS
    shown_at: float | None = None
    # How many events a merged burst card stands for.
    count: int = 1
    created_at: float = 0.0


@dataclass
class Mirror:
    """The bridge's copy of the server state, fed by events."""

    online: bool = True
    players: int | None = None
    max_players: int | None = None
    tps: float | None = None
    mspt: float | None = None
    ticks: int | None = None
    ticks_at: float = 0.0
    sleeping: int = 0
    sleep_needed: int = 1
    # The vane-regions region the player is in, as sent in `region.enter`.
    region: dict[str, Any] | None = None
    autostop_end: float | None = None
    autostop_total: float = 1.0
    # The server that is stopping, behind a vane-velocity proxy; None on a single server.
    autostop_server: str | None = None
    busy: bool = False
    permissions: set[str] = field(default_factory=set)
    icon_tag: str | None = None
    slime_chunk: bool = False


TICKS_PER_SECOND = 20


def is_night(ticks: float) -> bool:
    """Same night window Minecraft uses for sleeping."""
    return 12542 <= ticks % 24000 < 23460


def clock_of(ticks: float, step: int = 1) -> str:
    """Minecraft ticks as a 24-hour clock, rounded down to `step` minutes; tick 0 is 06:00."""
    t = ticks % 24000
    hours = int((t / 1000 + 6) % 24)
    minutes = int((t % 1000) / 1000 * 60) // step * step
    return f"{hours:02d}:{minutes:02d}"


def mmss(seconds: float) -> str:
    """Seconds as m:ss, rounded up so a countdown never shows 0:00 early."""
    s = max(0, math.ceil(seconds))
    return f"{s // 60}:{s % 60:02d}"


def region_card(d: dict[str, Any]) -> Card:
    """The card for the region in `d`: whose it is, or that it is a safe zone."""
    if d.get("own"):
        status, color = "YOUR LAND", GREEN
    elif d.get("pvp") is False:
        status, color = "SAFE ZONE", GREEN
    elif d.get("owner"):
        status, color = "BY " + d["owner"], GREY
    else:
        status, color = "REGION", GREY
    return Card("shield" if d.get("may_build") else "shield-denied", d["region"], status, color2=color)


class BarState:
    """
    Turns events into screens.

    `send(type, payload)` is called for requests to the server (focus mode, abort autostop).
    `on_icon_changed(tag)` is called when the snapshot announces a new server icon.
    """

    def __init__(
        self,
        send: Callable[[dict[str, Any]], None],
        on_icon_changed: Callable[[str | None], None] = lambda tag: None,
    ) -> None:
        self.m = Mirror()
        self._send = send
        self._on_icon_changed = on_icon_changed
        self.queue: list[Alert] = []
        self.current: Alert | None = None
        self.held: list[Alert] = []
        self.focus_since = 0.0
        self.ambient_index = 0
        self.ambient_since = 0.0
        self.ambient_id: str | None = None
        self._recent: dict[str, list[tuple[float, int]]] = {}
        self._now = 0.0
        self.dropped = 0

    # ------------------------------------------------------------------ events

    def handle(self, kind: str, data: dict[str, Any], now: float) -> None:
        """Apply one server event."""
        self._now = now
        handler = getattr(self, "_on_" + kind.replace(".", "_"), None)
        if handler is not None:
            handler(data, now)

    def _on_snapshot(self, d: dict[str, Any], now: float) -> None:
        m = self.m
        m.online = bool(d.get("online", True))
        m.players = d.get("players")
        m.max_players = d.get("max")
        m.tps = d.get("tps")
        m.mspt = d.get("mspt")
        if "ticks" in d:
            m.ticks, m.ticks_at = d["ticks"], now
        m.sleeping = d.get("sleeping", 0)
        m.sleep_needed = d.get("sleep_needed", 1)
        m.busy = bool(d.get("busy", False))
        m.permissions = set(d.get("permissions", []))
        m.slime_chunk = bool(d.get("slime_chunk", False))
        m.region = d.get("region")
        remaining = d.get("autostop_remaining_s")
        m.autostop_end = now + remaining if remaining is not None else None
        m.autostop_total = max(1.0, float(remaining or 1))
        m.autostop_server = d.get("server")
        if d.get("icon") != m.icon_tag:
            m.icon_tag = d.get("icon")
            self._on_icon_changed(m.icon_tag)

    def _on_permissions_updated(self, d: dict[str, Any], now: float) -> None:
        self.m.permissions = set(d.get("granted", []))

    def _on_player_join(self, d: dict[str, Any], now: float) -> None:
        self.m.online, self.m.players, self.m.max_players = True, d.get("online"), d.get("max")
        self.alert("player.join", Card(head_icon(d["player"], d.get("head")), d["player"], "JOINED", color2=GREEN))

    def _on_player_quit(self, d: dict[str, Any], now: float) -> None:
        self.m.players = d.get("online")
        self.alert("player.quit", Card(head_icon(d["player"], d.get("head")), d["player"], "LEFT", color2=GREY))

    def _on_world_time(self, d: dict[str, Any], now: float) -> None:
        self.m.ticks, self.m.ticks_at = d["ticks"], now

    def _on_bedtime_enter(self, d: dict[str, Any], now: float) -> None:
        self.m.sleeping, self.m.sleep_needed = d["sleeping"], d["needed"]
        self.alert("bedtime.enter", Card("bed", d["player"], f"SLEEPS {d['sleeping']}/{d['needed']}", color2=CYAN), 2)

    def _on_bedtime_leave(self, d: dict[str, Any], now: float) -> None:
        self.m.sleeping, self.m.sleep_needed = d["sleeping"], d["needed"]

    def _on_bedtime_skip(self, d: dict[str, Any], now: float) -> None:
        self.m.sleeping = 0
        if "ticks" in d:
            self.m.ticks, self.m.ticks_at = d["ticks"], now
        self.alert("bedtime.skip", Card("sun", "GOOD MORNING", "NIGHT SKIPPED", color1=AMBER), 3)

    def _on_presence_updated(self, d: dict[str, Any], now: float) -> None:
        busy = bool(d.get("busy"))
        if busy != self.m.busy:
            self._apply_focus(busy, now)

    def _on_portal_activate(self, d: dict[str, Any], now: float) -> None:
        self.alert("portal.activate", Card("portal", "> " + d["to"], d["player"], color1=PURPLE), 2)

    def _on_portal_destroy(self, d: dict[str, Any], now: float) -> None:
        self.alert("portal.destroy", Card("portal-broken", d["portal"], "BROKEN BY " + d["by"], color1=RED), 3)

    def _on_advancement(self, d: dict[str, Any], now: float) -> None:
        frame = d.get("frame", "task")
        color, priority = {"goal": (CYAN, 2), "challenge": (PURPLE, 3)}.get(frame, (GREEN, 1))
        count = int(d.get("count", 1))
        title = d["title"] if count == 1 else f"{count} ADVANCEMENTS"
        self.alert("advancement", Card(head_icon(d["player"], d.get("head")), title, d["player"], color1=color),
                   priority, count=count)

    def _on_slimechunk_enter(self, d: dict[str, Any], now: float) -> None:
        self.m.slime_chunk = True
        self.alert("slimechunk.enter", Card("slime", "SLIME CHUNK", "SLIMES SPAWN HERE", color1=GREEN), 2)

    def _on_slimechunk_leave(self, d: dict[str, Any], now: float) -> None:
        self.m.slime_chunk = False

    def _on_region_enter(self, d: dict[str, Any], now: float) -> None:
        self.m.region = d
        self.alert("region.enter", region_card(d))

    def _on_region_leave(self, d: dict[str, Any], now: float) -> None:
        self.m.region = None

    def _on_region_visitor(self, d: dict[str, Any], now: float) -> None:
        names, count, where = d.get("visitors") or ["?"], int(d.get("count", 1)), "IN " + d["region"]
        if count == 1:
            card = Card(head_icon(names[0], d.get("head")), names[0], where, color2=AMBER)
        elif count <= 3:
            card = Card(head_icon(names[0], d.get("head")), f"{names[0]} +{count - 1}", where, color2=AMBER)
        else:
            card = Card("shield-denied", f"{count} VISITORS", where, color1=AMBER)
        self.alert("region.visitor", card, 2, count=count)

    def _on_server_tps(self, d: dict[str, Any], now: float) -> None:
        self.m.tps, self.m.mspt = d["tps"], d["mspt"]
        if d["tps"] < 17:
            self.alert("server.tps", Card("warn", f"LAG {d['tps']:.1f} TPS", f"{round(d['mspt'])} MSPT", color1=AMBER), 3)

    def _on_autostop_scheduled(self, d: dict[str, Any], now: float) -> None:
        self.m.autostop_end = now + d["remaining_s"]
        self.m.autostop_total = max(1.0, float(d["remaining_s"]))
        self.m.autostop_server = d.get("server")
        self.alert("autostop.scheduled", Card("clock-red", "SERVER EMPTY", f"STOP IN {mmss(d['remaining_s'])}", color1=RED), CRITICAL, 3.5)

    def _on_autostop_aborted(self, d: dict[str, Any], now: float) -> None:
        self.m.autostop_end = None
        how = "VIA BUSY BAR" if d.get("by", "").startswith("busybar:") else "CANCELLED"
        self.alert("autostop.aborted", Card("clock", "AUTOSTOP OFF", how, color1=GREEN), 3)

    def _on_server_stop(self, d: dict[str, Any], now: float) -> None:
        self.m.online = False
        self.m.autostop_end = None
        # Whatever was queued happened on a server that is now gone.
        self.queue = [a for a in self.queue if a.priority >= CRITICAL]
        self.dropped = 0
        if self.current is not None and self.current.priority < CRITICAL:
            self.current = None
        self.alert("server.stop", Card("server-grey", "SERVER", "STOPPED", color2=RED), CRITICAL)

    # ------------------------------------------------------------------ alerts

    def alert(self, kind: str, card: Card, priority: int = 1, duration: float | None = None, count: int = 1) -> None:
        """
        Queue a card, or hold it while focus mode is on and it is not critical. More than
        BURST_THRESHOLD non-critical alerts of one kind within BURST_WINDOW fold into a single
        card that counts up while the burst lasts.
        """
        now = self._now
        a = Alert(kind, card, priority, duration or (5.0 if priority >= CRITICAL else ALERT_SECONDS),
                  count=count, created_at=now)
        if self.m.busy and priority < CRITICAL:
            self.held.append(a)
            return
        if priority < CRITICAL and self._fold_into_burst(a, now):
            return
        self._enqueue(a)

    def _fold_into_burst(self, a: Alert, now: float) -> bool:
        """Merge `a` into a burst card of its kind; False while it is still a single alert."""
        recent = [(t, n) for t, n in self._recent.get(a.kind, []) if now - t <= BURST_WINDOW] + [(now, a.count)]
        self._recent[a.kind] = recent
        burst = a.kind + ".burst"
        alerts = ([self.current] if self.current else []) + self.queue
        summary = next((q for q in alerts if q.kind == burst), None)
        if summary is None and len(recent) <= BURST_THRESHOLD:
            return False
        if summary is not None:
            summary.count += a.count
            summary.priority = max(summary.priority, a.priority)
        else:
            # Start with everything in the window, and drop the single alerts it replaces.
            self.queue = [q for q in self.queue if q.kind != a.kind]
            if self.current is not None and self.current.kind == a.kind:
                self.current = None
            summary = Alert(burst, a.card, a.priority, count=sum(n for _, n in recent), created_at=now)
            self._enqueue(summary)
        summary.card = self._summary_card(a.kind, summary.count, a.card)
        if summary.shown_at is not None:
            # Keep it up while the burst goes on.
            summary.duration = now - summary.shown_at + ALERT_SECONDS
        return True

    def _summary_card(self, kind: str, count: int, last: Card) -> Card:
        """The counting card for a burst of `kind`; `last` is the newest single card."""
        m = self.m
        online = f"ONLINE {m.players}/{m.max_players}" if m.players is not None else ""
        if kind == "player.join":
            return Card("server", f"{count} JOINED", online, color1=GREEN)
        if kind == "player.quit":
            return Card("server", f"{count} LEFT", online, color1=GREY)
        if kind == "advancement":
            return Card("server", f"{count} ADVANCEMENTS", "LAST " + last.line2, color1=last.color1)
        if kind == "bedtime.enter":
            return Card("bed", f"{count} IN BED", last.line2, color2=CYAN)
        if kind == "region.visitor":
            return Card("shield-denied", f"{count} VISITORS", last.line2, color1=AMBER)
        return Card(last.icon, f"{count}X {last.line1}", last.line2, color1=last.color1, color2=last.color2)

    def _enqueue(self, a: Alert) -> None:
        priority = a.priority
        if self.current is None or priority > self.current.priority:
            if self.current is not None:
                self.current.shown_at = None
                self.queue.insert(0, self.current)
            self.current = a
        else:
            index = next((i for i, q in enumerate(self.queue) if q.priority < priority), len(self.queue))
            self.queue.insert(index, a)
        # The queue is sorted by priority, so the tail holds the least important, newest alerts.
        while len(self.queue) > MAX_QUEUE:
            victim = next((q for q in reversed(self.queue) if q.priority < CRITICAL), None)
            if victim is None:
                break
            self.queue.remove(victim)
            self.dropped += victim.count

    def _current_alert(self, now: float) -> Alert | None:
        if self.current is not None and self.current.shown_at is not None and now - self.current.shown_at > self.current.duration:
            self.current = None
            self.ambient_since = now
        while self.current is None and self.queue:
            candidate = self.queue.pop(0)
            if candidate.priority < CRITICAL and now - candidate.created_at > MAX_WAIT_SECONDS:
                self.dropped += candidate.count
                continue
            self.current = candidate
        if self.current is None and self.dropped and not self.m.busy:
            skipped, self.dropped = self.dropped, 0
            self.current = Alert("overflow", Card("warn", f"+{skipped} MORE", "ALERTS SKIPPED", color1=AMBER), created_at=now)
        if self.current is not None and self.current.shown_at is None:
            self.current.shown_at = now
        return self.current

    def needs_face(self, skin: str) -> bool:
        """Whether an alert that will still be shown uses the face of skin `skin`."""
        alerts = ([self.current] if self.current else []) + self.queue
        return any(a.card.icon.endswith(":" + skin) for a in alerts)

    # ------------------------------------------------------------------ screens

    def ambient_ids(self, now: float) -> list[str]:
        """Idle screens, in rotation order. Some states pin a single screen."""
        m = self.m
        if not m.online:
            return ["offline"]
        if m.autostop_end is not None:
            return ["autostop"]
        if m.busy:
            return ["focus"]
        ids = []
        if m.players is not None:
            ids.append("players")
        if m.ticks is not None:
            ids.append("world")
            if m.sleeping > 0 and is_night(self._ticks(now)):
                ids.append("bedtime")
        if m.slime_chunk:
            ids.append("slime")
        if m.region is not None:
            ids.append("region")
        return ids or ["players"]

    def _ticks(self, now: float) -> float:
        return (self.m.ticks or 0) + (now - self.m.ticks_at) * TICKS_PER_SECOND

    def ambient_card(self, screen: str, now: float) -> Card:
        m = self.m
        if screen == "players":
            if m.players is None:
                return Card("server-grey", "CONNECTING", color1=GREY)
            count = f"{m.players}/{m.max_players}"
            if m.tps is None:
                # No permission for server performance: spend both lines on the count.
                return Card("server", "ONLINE", count, color2=GREEN)
            tps_color = GREEN if m.tps >= 18 else AMBER if m.tps >= 15 else RED
            return Card("server", f"ONLINE {count}", f"TPS {m.tps:.1f}", color2=tps_color)
        if screen == "world":
            t = self._ticks(now)
            night = is_night(t)
            # Ten-minute steps: an in-game minute is under a second, too often to redraw the Bar.
            return Card("moon" if night else "sun", f"TIME {clock_of(t, step=10)}", "NIGHT" if night else "DAY",
                        color2=CYAN if night else AMBER)
        if screen == "bedtime":
            left = m.sleep_needed - m.sleeping
            return Card("bed", f"ZZZ {m.sleeping}/{m.sleep_needed}", "SKIPPING..." if left <= 0 else f"NEED {left} MORE",
                        color1=CYAN, bar=m.sleeping / max(1, m.sleep_needed), bar_color=CYAN)
        if screen == "slime":
            return Card("slime", "SLIME CHUNK", "YOU'RE IN ONE", color1=GREEN, color2=GREY)
        if screen == "region" and m.region is not None:
            return region_card(m.region)
        if screen == "autostop":
            remaining = (m.autostop_end or now) - now
            can_abort = "action.autostop_abort" in m.permissions
            return Card("clock-red", f"STOP IN {mmss(remaining)}", "OK = ABORT" if can_abort else "SERVER EMPTY",
                        color1=RED, bar=remaining / m.autostop_total, bar_color=RED)
        if screen == "focus":
            left = FOCUS_SECONDS - (now - self.focus_since)
            return Card("busy", f"FOCUS {mmss(left)}" if left > 0 else "BREAK TIME",
                        f"{len(self.held)} HELD" if self.held else "MC MUTED", color1=RED, color2=GREY)
        return Card("server-grey", "SERVER OFF", "WAITING...", color1=GREY, color2=GREY)

    def frame(self, now: float) -> tuple[Card, str]:
        """The card to show now, and a short label for logs."""
        alert = self._current_alert(now)
        if alert is not None:
            return alert.card, f"alert {alert.kind}"
        ids = self.ambient_ids(now)
        if now - self.ambient_since > ROTATE_SECONDS:
            self.ambient_index += 1
            self.ambient_since = now
        screen = ids[self.ambient_index % len(ids)]
        if screen != self.ambient_id:
            self.ambient_id, self.ambient_since = screen, now
        return self.ambient_card(screen, now), screen

    # ------------------------------------------------------------------ inputs

    def press_ok(self, now: float) -> None:
        """OK aborts a pending autostop when allowed, otherwise dismisses the alert."""
        if self.m.autostop_end is not None and "action.autostop_abort" in self.m.permissions:
            request = {"type": "action.invoke", "action": "autostop.abort"}
            # Behind a proxy, name the server, since several may be stopping at once.
            if self.m.autostop_server is not None:
                request["server"] = self.m.autostop_server
            self._send(request)
        else:
            self.press_back(now)

    def press_back(self, now: float) -> None:
        """Dismiss the current alert."""
        if self.current is not None:
            self.current = None
            self.ambient_since = now

    def turn_wheel(self, delta: int, now: float) -> None:
        """Flip through the idle screens."""
        if self.current is None:
            self.ambient_index += delta
            self.ambient_since = now
            self.ambient_id = None

    def set_focus(self, busy: bool, now: float) -> None:
        """Change focus mode from the Bar and report it to the server."""
        if busy == self.m.busy:
            return
        self._apply_focus(busy, now)
        if "action.presence" in self.m.permissions:
            self._send({"type": "presence.set", "busy": busy})

    def _apply_focus(self, busy: bool, now: float) -> None:
        self._now = now
        self.m.busy = busy
        self.ambient_since = now
        if busy:
            self.focus_since = now
            self.held = []
            if self.current is not None and self.current.priority < CRITICAL:
                self.current = None
            self.queue = [q for q in self.queue if q.priority >= CRITICAL]
        elif self.held:
            count, last = len(self.held), self.held[-1]
            self.held = []
            self.alert("focus.digest", Card("busy", f"{count} WHILE BUSY", f"LAST {last.card.line1}", color1=AMBER), 2, 5.0)
