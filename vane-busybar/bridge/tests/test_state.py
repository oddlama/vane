from barmc.state import CRITICAL, BarState


def make(permissions=("receive.players", "action.presence")):
    sent = []
    icons = []
    s = BarState(send=sent.append, on_icon_changed=icons.append)
    s.handle(
        "snapshot",
        {
            "online": True,
            "players": 3,
            "max": 20,
            "tps": 19.9,
            "mspt": 21.0,
            "ticks": 6000,
            "sleeping": 0,
            "sleep_needed": 2,
            "busy": False,
            "permissions": list(permissions),
            "icon": '"abc"',
        },
        0.0,
    )
    return s, sent, icons


def test_snapshot_shows_players_and_announces_icon():
    s, _, icons = make()
    card, label = s.frame(0.0)
    assert label == "players"
    assert (card.icon, card.line1, card.line2) == ("server", "ONLINE 3/20", "TPS 19.9")
    assert icons == ['"abc"']


def test_higher_priority_alert_interrupts_and_old_one_resumes():
    s, _, _ = make()
    s.handle("player.join", {"player": "Bex", "online": 4, "max": 20}, 1.0)
    assert s.frame(1.0)[1] == "alert player.join"
    s.handle("portal.destroy", {"portal": "Base", "by": "Otto"}, 1.5)
    assert s.frame(1.5)[1] == "alert portal.destroy"
    assert s.frame(10.0)[1] == "alert player.join"


def test_focus_holds_non_critical_alerts_and_summarises_them():
    s, sent, _ = make()
    s.set_focus(True, 1.0)
    assert sent == [{"type": "presence.set", "busy": True}]
    s.handle("player.join", {"player": "Bex", "online": 4, "max": 20}, 2.0)
    assert s.frame(2.0)[1] == "focus"
    s.handle("autostop.scheduled", {"remaining_s": 30}, 3.0)
    assert s.frame(3.0)[0].line1 == "SERVER EMPTY"
    s.set_focus(False, 20.0)
    card, label = s.frame(20.0)
    assert label == "alert focus.digest" and card.line1 == "1 WHILE BUSY"


def test_focus_is_not_sent_without_permission():
    s, sent, _ = make(permissions=())
    s.set_focus(True, 1.0)
    assert sent == [] and s.m.busy


def test_ok_aborts_autostop_only_with_permission():
    s, sent, _ = make()
    s.handle("autostop.scheduled", {"remaining_s": 30}, 0.0)
    s.frame(0.0)  # the alert's lifetime starts when it is first shown
    assert s.frame(10.0)[0].line2 == "SERVER EMPTY"
    s.press_ok(10.0)
    assert sent == []

    s, sent, _ = make(permissions=("action.autostop_abort",))
    s.handle("autostop.scheduled", {"remaining_s": 30}, 0.0)
    s.frame(0.0)
    card, label = s.frame(10.0)
    assert label == "autostop" and card.line1 == "STOP IN 0:20" and card.line2 == "OK = ABORT"
    s.press_ok(10.0)
    assert sent == [{"type": "action.invoke", "action": "autostop.abort"}]


def test_server_stop_pins_offline_screen():
    s, _, _ = make()
    s.handle("server.stop", {"reason": "shutdown"}, 0.0)
    assert s.frame(0.0)[0].line2 == "STOPPED"
    assert s.frame(10.0)[1] == "offline"


def test_players_screen_without_tps_permission_and_before_snapshot():
    s = BarState(send=lambda r: None)
    assert s.frame(0.0)[0].line1 == "CONNECTING"
    s.handle("snapshot", {"players": 2, "max": 20, "permissions": []}, 0.0)
    card = s.frame(0.0)[0]
    assert (card.line1, card.line2) == ("ONLINE", "2/20")


def test_world_clock_moves_in_ten_minute_steps():
    from barmc.state import clock_of

    assert clock_of(15116) == "21:06" and clock_of(15116, step=10) == "21:00"


def test_join_alert_carries_the_skin_id():
    s, _, _ = make()
    s.handle("player.join", {"player": "Bex", "online": 4, "max": 20, "head": "abc123"}, 0.0)
    assert s.frame(0.0)[0].icon == "head:Bex:abc123"
    s.handle("player.quit", {"player": "Otto", "online": 3, "max": 20, "head": None}, 0.0)
    assert s.queue[-1].card.icon == "head:Otto:"


def test_slime_chunk_alerts_and_joins_the_rotation_until_left():
    s, _, _ = make()
    s.handle("slimechunk.enter", {"world": "world", "chunk_x": 1, "chunk_z": 2}, 0.0)
    card, label = s.frame(0.0)
    assert label == "alert slimechunk.enter" and card.icon == "slime"
    assert "slime" in s.ambient_ids(10.0)
    s.handle("slimechunk.leave", {}, 11.0)
    assert "slime" not in s.ambient_ids(11.0)


def test_snapshot_restores_slime_chunk_state():
    s = BarState(send=lambda r: None)
    s.handle("snapshot", {"players": 1, "max": 20, "slime_chunk": True}, 0.0)
    assert "slime" in s.ambient_ids(0.0)


def test_wheel_flips_idle_screens():
    s, _, _ = make()
    assert s.frame(0.0)[1] == "players"
    s.turn_wheel(1, 1.0)
    assert s.frame(1.0)[1] == "world"
    assert CRITICAL == 4


def _quit(s, name, online, now):
    s.handle(
        "player.quit",
        {"player": name, "online": online, "max": 20, "head": "skin" + name},
        now,
    )


def test_mass_disconnect_becomes_one_counting_card():
    s, _, _ = make()
    s.handle("snapshot", {"players": 30, "max": 20, "permissions": []}, 0.0)
    for i in range(3):
        _quit(s, f"P{i}", 29 - i, 0.1 * i)
    assert s.frame(0.3)[1] == "alert player.quit"  # still individual below the threshold
    for i in range(3, 30):
        _quit(s, f"P{i}", 29 - i, 0.3 + 0.01 * i)
    card, label = s.frame(1.0)
    assert label == "alert player.quit.burst"
    assert (card.icon, card.line1, card.line2) == ("server", "30 LEFT", "ONLINE 0/20")
    assert [a.kind for a in s.queue] == []  # the single alerts were folded in
    assert not s.needs_face("skinP29") and s.frame(5.5)[1] != "alert player.quit.burst"


def test_burst_card_stays_up_while_the_burst_continues():
    s, _, _ = make()
    for i in range(5):
        _quit(s, f"P{i}", 10, 0.0)
    s.frame(0.0)
    _quit(s, "P5", 9, 3.0)
    assert s.frame(6.0)[0].line1 == "6 LEFT"  # 6 s after it appeared, extended by the late quit
    assert s.frame(7.0)[1] != "alert player.quit.burst"


def test_single_quits_still_fetch_faces():
    s, _, _ = make()
    _quit(s, "Mira", 2, 0.0)
    assert s.needs_face("skinMira")


def test_server_stop_drops_stale_alerts():
    s, _, _ = make()
    s.handle("player.join", {"player": "Bex", "online": 2, "max": 20}, 0.0)
    s.handle("portal.activate", {"player": "Bex", "to": "Hub"}, 0.0)
    s.handle("server.stop", {"reason": "shutdown"}, 0.1)
    assert s.frame(0.1)[1] == "alert server.stop"
    assert s.queue == []
    assert s.frame(6.0)[1] == "offline"


def test_advancement_shows_title_player_and_frame_colour():
    from barmc.sprites import PURPLE

    s, _, _ = make()
    s.handle(
        "advancement",
        {
            "player": "Mira",
            "head": "abc",
            "title": "Sweet Dreams",
            "frame": "challenge",
            "count": 1,
        },
        0.0,
    )
    card, label = s.frame(0.0)
    assert label == "alert advancement"
    assert (card.icon, card.line1, card.line2, card.color1) == (
        "head:Mira:abc",
        "Sweet Dreams",
        "Mira",
        PURPLE,
    )
    assert s.current.priority == 3


def test_advancement_batch_from_the_server_shows_its_count():
    s, _, _ = make()
    s.handle(
        "advancement",
        {"player": "Mira", "title": "Stone Age", "frame": "task", "count": 120},
        0.0,
    )
    assert s.frame(0.0)[0].line1 == "120 ADVANCEMENTS"


def test_any_kind_bursts_and_counts_items():
    s, _, _ = make()
    for i in range(5):
        s.handle(
            "advancement",
            {"player": f"P{i}", "title": "Stone Age", "frame": "task", "count": 2},
            0.1 * i,
        )
    card, label = s.frame(1.0)
    assert label == "alert advancement.burst" and card.line1 == "10 ADVANCEMENTS" and card.line2 == "LAST P4"
    for i in range(4):
        s.handle("portal.activate", {"player": "Bex", "to": "Hub"}, 1.0)
    # Portal trips outrank advancements, so their burst card takes the screen.
    assert s.current.kind == "portal.activate.burst" and s.current.card.line1 == "4X > Hub"
    assert s.queue[0].kind == "advancement.burst"


def test_queue_is_capped_and_skipped_alerts_are_reported():
    from barmc.state import MAX_QUEUE

    s, _, _ = make()
    # Distinct kinds so nothing merges: 1 showing + MAX_QUEUE queued, the rest dropped.
    for i in range(MAX_QUEUE + 4):
        s.alert(f"custom.{i}", s.ambient_card("players", 0.0), 1)
    assert len(s.queue) == MAX_QUEUE and s.dropped == 3
    s.handle("autostop.scheduled", {"remaining_s": 30}, 0.0)
    assert s.current.kind == "autostop.scheduled"  # critical alerts are never dropped


def test_stale_alerts_are_skipped_then_summarised():
    s, _, _ = make()
    s.handle("portal.activate", {"player": "Bex", "to": "Hub"}, 0.0)
    s.handle("player.join", {"player": "Otto", "online": 2, "max": 20}, 0.0)
    s.frame(0.0)
    card, label = s.frame(40.0)  # the queued join waited too long
    assert label == "alert overflow" and card.line1 == "+1 MORE"


def test_region_enter_alerts_and_adds_an_idle_screen_until_leaving():
    s, _, _ = make()
    s.handle(
        "region.enter",
        {
            "region": "Spawn",
            "owner": "Mira",
            "own": False,
            "pvp": False,
            "may_build": False,
        },
        1.0,
    )
    card, label = s.frame(1.0)
    assert label == "alert region.enter"
    assert (card.icon, card.line1, card.line2) == (
        "shield-denied",
        "Spawn",
        "SAFE ZONE",
    )
    assert "region" in s.ambient_ids(10.0)
    s.handle("region.leave", {"region": "Spawn"}, 11.0)
    assert "region" not in s.ambient_ids(11.0)


def test_region_card_says_whose_land_it_is():
    from barmc.state import region_card

    assert region_card({"region": "Base", "own": True, "pvp": True, "may_build": True}).line2 == "YOUR LAND"
    card = region_card(
        {
            "region": "Farm",
            "owner": "Otto",
            "own": False,
            "pvp": True,
            "may_build": True,
        }
    )
    assert (card.icon, card.line2) == ("shield", "BY Otto")


def test_snapshot_restores_the_current_region():
    s, _, _ = make()
    s.handle(
        "snapshot",
        {
            "online": True,
            "players": 1,
            "max": 20,
            "permissions": [],
            "region": {"region": "Base", "own": True, "pvp": True, "may_build": True},
        },
        0.0,
    )
    assert s.ambient_card("region", 0.0).line1 == "Base"


def test_visitor_cards_name_one_to_three_and_count_more():
    s, _, _ = make()
    s.handle(
        "region.visitor",
        {"region": "Base", "visitors": ["Steve"], "count": 1, "head": "abc"},
        1.0,
    )
    card = s.frame(1.0)[0]
    assert (card.icon, card.line1, card.line2) == ("head:Steve:abc", "Steve", "IN Base")
    s.press_back(2.0)
    s.handle(
        "region.visitor",
        {"region": "Base", "visitors": ["Alex", "Sam"], "count": 2, "head": None},
        70.0,
    )
    assert s.frame(70.0)[0].line1 == "Alex +1"
    s.press_back(71.0)
    s.handle(
        "region.visitor",
        {"region": "Base", "visitors": ["A", "B", "C", "D", "E"], "count": 7},
        140.0,
    )
    assert s.frame(140.0)[0].line1 == "7 VISITORS"


def test_visitor_bursts_across_regions_fold_into_one_count():
    s, _, _ = make()
    for i, region in enumerate(["Base", "Farm", "Mine", "Tower"]):
        s.handle(
            "region.visitor",
            {"region": region, "visitors": [f"P{i}"], "count": 1},
            1.0 + i * 0.1,
        )
    card, label = s.frame(2.0)
    assert label == "alert region.visitor.burst"
    assert card.line1 == "4 VISITORS"


def test_focus_mode_holds_visitor_alerts():
    s, _, _ = make()
    s.set_focus(True, 1.0)
    s.handle("region.visitor", {"region": "Base", "visitors": ["Steve"], "count": 1}, 2.0)
    assert s.frame(2.0)[1] == "focus"
    assert len(s.held) == 1


def test_ok_names_the_stopping_server_behind_a_proxy():
    s, sent, _ = make(permissions=("action.autostop_abort",))
    s.handle("autostop.scheduled", {"remaining_s": 30, "server": "survival"}, 0.0)
    s.frame(0.0)
    s.frame(10.0)
    s.press_ok(10.0)
    assert sent == [{"type": "action.invoke", "action": "autostop.abort", "server": "survival"}]
