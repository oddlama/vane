import io

from busylib import types
from PIL import Image

from barmc.display.busybar import build_elements
from barmc.display.terminal import render
from barmc.icons import (
    Library,
    face_grid,
    head_icon,
    resolve,
    shrink_server_icon,
    to_png,
)
from barmc.state import Card


def test_busybar_elements_validate_and_long_lines_scroll():
    elements = build_elements(Card("bed", "MIRA", "BROKEN BY A CREEPER"), "abc.png")
    payload = types.DisplayElements(application_name="barmc", elements=elements).model_dump(exclude_none=True)
    icon, top, bottom = payload["elements"]
    assert icon == {
        "id": "icon",
        "type": "image",
        "path": "abc.png",
        "x": 1,
        "y": 8,
        "align": "mid_left",
        "display": "front",
        "opacity": 100,
    }
    assert top["y"] == 0 and top["align"] == "top_left" and "scroll_rate" not in top
    assert bottom["y"] == 16 and bottom["scroll_rate"] == 1200 and bottom["width"] == 57
    assert top["color"] == "#F3EFE4FF"


def test_server_icon_shrinks_with_transparency():
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for x in range(32, 64):
        for y in range(64):
            image.putpixel((x, y), (255, 0, 0, 255))
    out = io.BytesIO()
    image.save(out, format="PNG")
    grid = shrink_server_icon(out.getvalue())
    assert grid[0][0] is None and grid[0][11] == "#ff0000"
    assert resolve("server", grid) is grid
    assert resolve("server", None)[0][0] == "#5cb83a"


def test_busybar_always_sends_all_three_elements():
    elements = build_elements(Card("sun", "GOOD MORNING"), "abc.png")
    assert [e.id for e in elements] == ["icon", "line1", "line2"] and elements[2].text == " "


def test_png_roundtrip_and_terminal_render():
    png = to_png(resolve("head:Mira", None))
    assert Image.open(io.BytesIO(png)).size == (12, 12)
    fb = render(Card("sun", "GOOD MORNING", "NIGHT SKIPPED"), 0.0, Library())
    assert len(fb) == 16 and len(fb[0]) == 72 and any(fb[2][16:])


def _face_png():
    image = Image.new("RGBA", (8, 8), (200, 120, 60, 255))
    image.putpixel((0, 0), (0, 0, 255, 255))
    out = io.BytesIO()
    image.save(out, format="PNG")
    return out.getvalue()


def test_face_doubles_to_16_and_falls_back_until_fetched():
    grid = face_grid(_face_png())
    assert len(grid) == 16 and len(grid[0]) == 16
    assert grid[0][0] == grid[1][1] == "#0000ff" and grid[2][2] == "#c8783c"

    library = Library()
    icon = head_icon("Mira", "abc123")
    assert len(library.resolve(icon)) == 12  # generic head before the face arrives
    before = library.version
    library.add_face("abc123", grid)
    assert library.resolve(icon) is grid and library.version != before


def test_face_cards_move_the_text_right():
    elements = build_elements(Card("head:Mira:abc", "MIRA", "JOINED"), "f.png", icon_width=16)
    assert (elements[0].x, elements[1].x) == (0, 18)
    library = Library()
    library.add_face("abc", face_grid(_face_png()))
    fb = render(Card("head:Mira:abc", "MIRA", "JOINED"), 0.0, library)
    assert fb[0][0] == "#0000ff" and fb[15][15] == "#c8783c" and fb[2][16] is None
