"""Pixel art shared by the renderers: a 3x5 font and 12x12 icons, same as the web demo."""

from __future__ import annotations

WHITE = "#f3efe4"
GREEN = "#6fe06b"
RED = "#ff4f45"
AMBER = "#ffb03a"
CYAN = "#58d4ff"
PURPLE = "#b67dff"
GREY = "#7d7f82"

_FONT_SRC = {
    "A": ".#.|#.#|###|#.#|#.#",
    "B": "##.|#.#|##.|#.#|##.",
    "C": ".##|#..|#..|#..|.##",
    "D": "##.|#.#|#.#|#.#|##.",
    "E": "###|#..|##.|#..|###",
    "F": "###|#..|##.|#..|#..",
    "G": ".##|#..|#.#|#.#|.##",
    "H": "#.#|#.#|###|#.#|#.#",
    "I": "###|.#.|.#.|.#.|###",
    "J": "..#|..#|..#|#.#|.#.",
    "K": "#.#|#.#|##.|#.#|#.#",
    "L": "#..|#..|#..|#..|###",
    "M": "#.#|###|###|#.#|#.#",
    "N": "##.|#.#|#.#|#.#|#.#",
    "O": ".#.|#.#|#.#|#.#|.#.",
    "P": "##.|#.#|##.|#..|#..",
    "Q": ".#.|#.#|#.#|##.|.##",
    "R": "##.|#.#|##.|#.#|#.#",
    "S": ".##|#..|.#.|..#|##.",
    "T": "###|.#.|.#.|.#.|.#.",
    "U": "#.#|#.#|#.#|#.#|###",
    "V": "#.#|#.#|#.#|#.#|.#.",
    "W": "#.#|#.#|###|###|#.#",
    "X": "#.#|#.#|.#.|#.#|#.#",
    "Y": "#.#|#.#|.#.|.#.|.#.",
    "Z": "###|..#|.#.|#..|###",
    "0": "###|#.#|#.#|#.#|###",
    "1": ".#.|##.|.#.|.#.|###",
    "2": "##.|..#|.#.|#..|###",
    "3": "##.|..#|.#.|..#|##.",
    "4": "#.#|#.#|###|..#|..#",
    "5": "###|#..|##.|..#|##.",
    "6": ".##|#..|###|#.#|###",
    "7": "###|..#|.#.|.#.|.#.",
    "8": "###|#.#|###|#.#|###",
    "9": "###|#.#|###|..#|##.",
    " ": "...|...|...|...|...",
    ":": "...|.#.|...|.#.|...",
    ".": "...|...|...|...|.#.",
    ",": "...|...|...|.#.|#..",
    "/": "..#|..#|.#.|#..|#..",
    "-": "...|...|###|...|...",
    ">": "#..|.#.|..#|.#.|#..",
    "<": "..#|.#.|#..|.#.|..#",
    "%": "#.#|..#|.#.|#..|#.#",
    "!": ".#.|.#.|.#.|...|.#.",
    "?": "##.|..#|.#.|...|.#.",
    "'": ".#.|.#.|...|...|...",
    "+": "...|.#.|###|.#.|...",
    "=": "...|###|...|###|...",
    "(": "..#|.#.|.#.|.#.|..#",
    ")": "#..|.#.|.#.|.#.|#..",
    "_": "...|...|...|...|###",
}
FONT: dict[str, list[str]] = {k: v.split("|") for k, v in _FONT_SRC.items()}


def text_width(text: str) -> int:
    """Width in pixels of `text` in the 3x5 font, with one pixel between glyphs."""
    return max(0, len(text) * 4 - 1)


ICONS: dict[str, tuple[dict[str, str], list[str]]] = {
    "grass": (
        {"g": "#5cb83a", "G": "#93e05f", "d": "#8a5a2b", "D": "#65401c"},
        [
            "gggggggggggg",
            "gggggggggggg",
            "gGgggGggGggg",
            "dgdddgdgdddg",
            "dddddddddddd",
            "ddDddddddDdd",
            "dddddDdddddd",
            "dddddddddddd",
            "dDdddddddDdd",
            "ddddddDddddd",
            "dddddddddddd",
            "dddddddddddd",
        ],
    ),
    "head": (
        {
            "h": "#5a3a1c",
            "s": "#e0a97a",
            "w": "#ffffff",
            "b": "#3c5fd6",
            "n": "#b77a52",
            "m": "#6b3b24",
        },
        [
            "hhhhhhhhhhhh",
            "hhhhhhhhhhhh",
            "hssssssssssh",
            "ssssssssssss",
            "ssssssssssss",
            "swbssssssbws",
            "swbssssssbws",
            "sssssnnsssss",
            "sssmssssmsss",
            "sssmmmmmmsss",
            "ssssssssssss",
            "ssssssssssss",
        ],
    ),
    "bed": (
        {"#": "#9a6a3a", "o": "#f4f1ea", "r": "#d8363a"},
        [
            "............",
            "............",
            "#...........",
            "#...........",
            "#.oo........",
            "#.oorrrrrrrr",
            "#rrrrrrrrrrr",
            "#rrrrrrrrrrr",
            "############",
            "#..........#",
            "#..........#",
            "............",
        ],
    ),
    "sun": (
        {"y": "#ffcc3a"},
        [
            "y....y....y.",
            ".y.......y..",
            "...yyyyy....",
            "..yyyyyyy...",
            "..yyyyyyy...",
            "yyyyyyyyyyy.",
            "..yyyyyyy...",
            "..yyyyyyy...",
            "...yyyyy....",
            ".y.......y..",
            "y....y....y.",
            "............",
        ],
    ),
    "moon": (
        {"w": "#dfe6ff", "s": "#8fa0d8"},
        [
            "...wwww.....",
            ".wwww.....s.",
            ".www........",
            "www.........",
            "www......s..",
            "www.........",
            "www.........",
            "wwww........",
            ".wwww.......",
            ".wwwwww.....",
            "...wwwwww...",
            "............",
        ],
    ),
    "clock": (
        {"y": "#ffb03a", "w": "#f3efe4"},
        [
            "...yyyyyy...",
            "..y......y..",
            ".y...w....y.",
            "y....w.....y",
            "y....w.....y",
            "y....wwww..y",
            "y..........y",
            "y..........y",
            ".y........y.",
            "..y......y..",
            "...yyyyyy...",
            "............",
        ],
    ),
    "warn": (
        {"y": "#ffb03a", "k": "#1a1206"},
        [
            ".....yy.....",
            "....yyyy....",
            "....ykky....",
            "...yykkyy...",
            "...yykkyy...",
            "..yyykkyyy..",
            "..yyykkyyy..",
            ".yyyyyyyyyy.",
            ".yyyykkyyyy.",
            "yyyyyyyyyyyy",
            "............",
            "............",
        ],
    ),
    "shield": (
        {"b": "#2d6e32", "w": "#57c95f"},
        [
            ".bbbbbbbbbb.",
            ".bwwwwwwwwb.",
            ".bwwwwwwwwb.",
            ".bwwwwwwwwb.",
            ".bwwwwwwwwb.",
            ".bwwwwwwwwb.",
            "..bwwwwwwb..",
            "..bwwwwwwb..",
            "...bwwwwb...",
            "....bwwb....",
            ".....bb.....",
            "............",
        ],
    ),
    "busy": (
        {"r": "#e8322a", "w": "#ffffff"},
        [
            "....rrrr....",
            "..rrrrrrrr..",
            ".rrrrrrrrrr.",
            ".rrrrrrrrrr.",
            "rrrrrrrrrrrr",
            "rwwwwwwwwwwr",
            "rwwwwwwwwwwr",
            "rrrrrrrrrrrr",
            ".rrrrrrrrrr.",
            ".rrrrrrrrrr.",
            "..rrrrrrrr..",
            "....rrrr....",
        ],
    ),
    "slime": (
        {"o": "#3f8f2e", "g": "#6fcf4f", "G": "#9be878", "k": "#1f3a17"},
        [
            "............",
            ".oooooooooo.",
            ".oGGggggggo.",
            ".oGgggggggo.",
            ".ogkkggkkgo.",
            ".ogkkggkkgo.",
            ".oggggggggo.",
            ".oggggggggo.",
            ".ogggkkgggo.",
            ".oggggggggo.",
            ".oooooooooo.",
            "............",
        ],
    ),
    "portal": (
        {"k": "#2a1450", "p": "#9150ff", "P": "#c9a0ff", "d": "#5b22c4"},
        [
            ".kkkkkkkkkk.",
            ".kpPdpPpdpk.",
            ".kdppPdpPpk.",
            ".kPpdpPdppk.",
            ".kpPdpdPpdk.",
            ".kdpPpPdpPk.",
            ".kpdPdppPdk.",
            ".kPpdpPdpPk.",
            ".kdPpdPpdpk.",
            ".kpdPpdPpdk.",
            ".kPpdPpdpPk.",
            ".kkkkkkkkkk.",
        ],
    ),
}

# Palette overrides for icon variants.
VARIANTS: dict[str, dict[str, str]] = {
    "shield-denied": {"w": "#ff5a4f", "b": "#7a1f1a"},
    "grass-grey": {"g": "#555555", "G": "#666666", "d": "#3a3a3a", "D": "#2e2e2e"},
    "portal-broken": {"k": "#7a1d18", "p": "#3b2a2a", "P": "#1a1a1a", "d": "#1a1a1a"},
    "clock-red": {"y": "#ff4f45"},
}

_HAIR = ["#5a3a1c", "#2a1b0e", "#c9a24a", "#8a2f1c", "#1d1d1d", "#d9cfbd", "#6b4a8a"]


def hair_for(name: str) -> str:
    """A stable hair colour for a player name, so heads tell players apart."""
    return _HAIR[sum(map(ord, name)) % len(_HAIR)]


def icon_pixels(name: str, overrides: dict[str, str] | None = None) -> list[list[str | None]]:
    """A 12x12 grid of hex colours (None for transparent) for icon `name`."""
    palette, rows = ICONS[name]
    palette = {**palette, **(overrides or {})}
    return [[None if ch == "." else palette[ch] for ch in row] for row in rows]
