import asyncio

from barmc.server import parse_sse


async def _lines(text):
    for line in text.split("\n"):
        yield line


def events(text):
    async def collect():
        return [e async for e in parse_sse(_lines(text))]

    return asyncio.run(collect())


def test_parses_events_and_skips_heartbeats():
    got = events(
        'id: 1\nevent: snapshot\ndata: {"online":true}\n\n'
        ": ping\n\n"
        'id: 2\nevent: player.join\ndata: {"player":"Mira"}\n\n'
    )
    assert [(e.kind, e.data) for e in got] == [
        ("snapshot", {"online": True}),
        ("player.join", {"player": "Mira"}),
    ]


def test_joins_multiline_data_and_ignores_bad_json():
    got = events('event: a\ndata: {"x":\ndata: 1}\n\nevent: b\ndata: nope\n\n')
    assert [(e.kind, e.data) for e in got] == [("a", {"x": 1})]
