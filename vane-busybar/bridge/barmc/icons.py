"""Icon images: resolving a card's icon name to pixels, shrinking the server icon, and PNG export."""

from __future__ import annotations

import io

from PIL import Image

from .sprites import VARIANTS, hair_for, icon_pixels

Grid = list[list[str | None]]
SIZE = 12


def shrink_server_icon(png: bytes) -> Grid:
    """
    Shrink the 64x64 server icon to 12x12 by area averaging. Pillow resizes RGBA with
    premultiplied alpha, so transparent edges do not bleed dark fringes; mostly transparent
    pixels become off LEDs.
    """
    image = Image.open(io.BytesIO(png)).convert("RGBA").resize((SIZE, SIZE), Image.Resampling.BOX)
    grid: Grid = []
    for y in range(SIZE):
        row: list[str | None] = []
        for x in range(SIZE):
            r, g, b, a = image.getpixel((x, y))
            row.append(None if a < 128 else f"#{r:02x}{g:02x}{b:02x}")
        grid.append(row)
    return grid


def greyed(grid: Grid) -> Grid:
    """A dimmed greyscale copy, used while the server is stopped."""

    def grey(color: str | None) -> str | None:
        if color is None:
            return None
        r, g, b = (int(color[i : i + 2], 16) for i in (1, 3, 5))
        level = round((0.3 * r + 0.59 * g + 0.11 * b) * 0.45)
        return f"#{level:02x}{level:02x}{level:02x}"

    return [[grey(c) for c in row] for row in grid]


def face_grid(png: bytes) -> Grid:
    """A player's 8x8 face from the plugin, doubled to 16x16 so it fills the Bar's height."""
    image = Image.open(io.BytesIO(png)).convert("RGBA")
    if image.size != (8, 8):
        image = image.resize((8, 8), Image.Resampling.NEAREST)
    grid: Grid = []
    for y in range(16):
        row: list[str | None] = []
        for x in range(16):
            r, g, b, a = image.getpixel((x // 2, y // 2))
            row.append(None if a < 128 else f"#{r:02x}{g:02x}{b:02x}")
        grid.append(row)
    return grid


def head_icon(player: str, skin: str | None) -> str:
    """Card icon name for a player's head; the skin id is optional."""
    return f"head:{player}:{skin or ''}"


class Library:
    """
    Images that arrive from the server at runtime: the server icon and player faces.
    `version` changes whenever one does, so displays know to redraw.
    """

    def __init__(self) -> None:
        self.server_icon: Grid | None = None
        self.faces: dict[str, Grid] = {}
        self.version = 0

    def set_server_icon(self, grid: Grid | None) -> None:
        self.server_icon = grid
        self.version += 1

    def add_face(self, skin: str, grid: Grid) -> None:
        self.faces[skin] = grid
        self.version += 1

    def resolve(self, name: str) -> Grid:
        return resolve(name, self.server_icon, self.faces)


def resolve(name: str, server_icon: Grid | None, faces: dict[str, Grid] | None = None) -> Grid:
    """
    Pixels for a card icon name such as `bed`, `shield-denied`, `server-grey` or
    `head:<player>:<skin id>`. Most icons are 12x12; a known player face is 16x16.
    """
    if name.startswith("head:"):
        player, _, skin = name[5:].partition(":")
        if skin and faces and skin in faces:
            return faces[skin]
        return icon_pixels("head", {"h": hair_for(player)})
    if name in ("server", "server-grey"):
        grey = name == "server-grey"
        if server_icon is None:
            return icon_pixels("grass", VARIANTS["grass-grey"] if grey else None)
        return greyed(server_icon) if grey else server_icon
    if name in VARIANTS:
        return icon_pixels(name.split("-")[0], VARIANTS[name])
    return icon_pixels(name)


def to_png(grid: Grid) -> bytes:
    """Encode a grid as an RGBA PNG."""
    image = Image.new("RGBA", (len(grid[0]), len(grid)), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, color in enumerate(row):
            if color is not None:
                image.putpixel(
                    (x, y),
                    (
                        int(color[1:3], 16),
                        int(color[3:5], 16),
                        int(color[5:7], 16),
                        255,
                    ),
                )
    out = io.BytesIO()
    image.save(out, format="PNG")
    return out.getvalue()
