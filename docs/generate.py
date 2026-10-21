#!/usr/bin/env python3
"""Build the feature site from checked-in content, assets and documented recipe data.

No Minecraft installation, live server, network access or generated config directory is required.
"""

import argparse
import json
import re
import shutil
import tomllib
from dataclasses import dataclass, field
from html import escape
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit
from typing import Any

import markdown

ROOT = Path(__file__).resolve().parent
STATIC_PATHS = ("assets", "images", "wiki", "css", "favicon.ico", "vane.png")
FRONTMATTER = re.compile(r"\A```toml\s*\n(.*?)\n```\s*\n---\s*\n(.*)", re.DOTALL)
ITEM = re.compile(r"{{ item:(\S*) }}")


@dataclass
class Feature:
    loaded_from: str
    metadata: dict
    html_content: str


@dataclass
class Context:
    templates: dict = field(default_factory=dict)
    content_settings: dict = field(default_factory=dict)
    features: dict = field(default_factory=dict)
    icons: dict = field(default_factory=dict)
    items: dict = field(default_factory=dict)


context = Context()


def asset_path(relative: str) -> Path:
    path = (ROOT / relative).resolve()
    if not any(path.is_relative_to(ROOT / name) for name in STATIC_PATHS) or not path.is_file():
        raise ValueError(f"Missing or unsafe static asset: {relative}")
    return path


def item_to_icon(item: str) -> str:
    try:
        icon = context.icons[item]
    except KeyError as error:
        raise ValueError(f"No documented icon for {item}; update data/icons.json") from error
    asset_path(icon)
    return icon


def item_to_inline_icon(item: str) -> str:
    html = (
        context.templates["inline-icon"]
        .replace("{{ icon }}", escape(item_to_icon(item), quote=True))
        .replace("{{ name }}", escape(item, quote=True))
    )
    # Multiline HTML breaks Markdown table rows after Prettier reformats the template.
    return re.sub(r"\s*\n\s*", " ", html)


def load_feature_markdown(path: Path, default_slug: str) -> Feature:
    match = FRONTMATTER.fullmatch(path.read_text(encoding="utf-8"))
    if not match:
        raise ValueError(f"{path}: expected TOML frontmatter followed by ---")
    metadata = tomllib.loads(match[1])
    metadata.setdefault("slug", default_slug)
    if "icon" in metadata:
        metadata["icon"] = item_to_icon(metadata["icon"])
    elif "itemlike" in metadata:
        metadata["icon"] = context.items[metadata["itemlike"]]["icon"]
        asset_path(metadata["icon"])
    else:
        raise ValueError(f"{path}: missing icon or itemlike")
    content = ITEM.sub(lambda m: item_to_inline_icon(m[1]), match[2])
    return Feature(str(path), metadata, markdown.markdown(content, extensions=["tables"]))


def replace_category_variables(text: str, category: dict) -> str:
    return text.replace("{{ category.id }}", escape(category["id"], quote=True)).replace(
        "{{ category.title }}", escape(category["title"])
    )


def get_item_data(key: str) -> dict:
    item, kind = key.rsplit(".", 1)
    # These are documentation identifiers, not paths into a live plugin configuration.
    return context.items[item][kind]


def remove_optional(where: str, what: str) -> str:
    """Remove a whole optional image/container, independent of template formatting."""
    token = re.escape(what)
    pattern = rf"<img\b[^>]*{token}[^>]*>|<div\b[^>]*>\s*{token}\s*</div>"
    result, count = re.subn(pattern, "", where)
    if count != 1:
        raise ValueError(f"Expected one optional image/container for {what}, found {count}")
    return result


def render_recipe(feature: Feature, recipe: dict[str, Any]) -> str:
    if recipe["type"] == "shaped":
        html = context.templates["shaped-recipe"]

        shape = "".join([row.ljust(3) for row in (recipe["shape"] + 3 * [""])[:3]])
        ingredients = {str(k): v for k, v in recipe["ingredients"].items()}
        assert len(shape) == 9
        for i, c in enumerate(shape):
            tag = f"{{{{ recipe.ingredients.{i} }}}}"
            if c == " ":
                html = remove_optional(html, tag)
            else:
                ingredient = ingredients[c]
                html = html.replace(tag, item_to_icon(ingredient))
                html = html.replace(f"{{{{ recipe.ingredients.{i}.name }}}}", ingredient)
    elif recipe["type"] == "shapeless":
        html = context.templates["shaped-recipe"]  # Abuse the template for shapeless recipes

        for i, ingredient in enumerate(recipe["ingredients"] + (9 - len(recipe["ingredients"])) * [None]):
            tag = f"{{{{ recipe.ingredients.{i} }}}}"
            if ingredient is None:
                html = remove_optional(html, tag)
            else:
                html = html.replace(tag, item_to_icon(ingredient))
                html = html.replace(f"{{{{ recipe.ingredients.{i}.name }}}}", ingredient)
    elif recipe["type"] == "smithing":
        html = context.templates["smithing-recipe"]

        html = html.replace("{{ recipe.base }}", item_to_icon(recipe["base"]))
        html = html.replace("{{ recipe.base.name }}", recipe["base"])

        html = html.replace("{{ recipe.addition }}", item_to_icon(recipe["addition"]))
        html = html.replace("{{ recipe.addition.name }}", recipe["addition"])
    else:
        raise ValueError(f"cannot render recipe of unknown type {recipe['type']}")

    result_icon = item_to_icon(recipe["result"])
    html = html.replace("{{ recipe.result }}", result_icon)
    html = html.replace("{{ recipe.result.name }}", recipe["result"])
    if "icon" not in feature.metadata:
        feature.metadata["icon"] = result_icon
    return html


def render_loot_table(loot: dict[str, Any]) -> str:
    html = context.templates["loot-table"]
    loot_badge = context.templates["badge"].replace("{{ color }}", "gray")

    table_rows = []
    for table in loot.values():
        where_rows = []
        for where in table["tables"]:
            where = where.removeprefix("minecraft:chests/")
            badge_html = '<div class="inline-flex m-1">'
            badge_html = badge_html + loot_badge.replace("{{ text }}", where)
            badge_html = badge_html + "</div>"
            where_rows.append(badge_html)
        where_rows = "\n".join(where_rows)

        col_html = context.templates["loot-table-col-where"]
        col_html = col_html.replace("{{ loot_table.n_rows }}", str(len(table["items"])))
        col_html = col_html.replace("{{ loot_table.where.rows }}", where_rows)

        for i, item in enumerate(table["items"]):
            row_html = context.templates["loot-table-row"]
            row_html = row_html.replace("{{ loot_table.row.item.name }}", item["item"])
            row_html = row_html.replace("{{ loot_table.row.item.icon }}", item_to_icon(item["item"]))
            row_html = row_html.replace("{{ loot_table.row.chance }}", f"{item['chance'] * 100.0:.2f}%")
            if item["amount_min"] == item["amount_max"]:
                amount = str(item["amount_min"])
            else:
                amount = f"{item['amount_min']} - {item['amount_max']}"
            row_html = row_html.replace("{{ loot_table.row.amount }}", amount)
            row_html = row_html.replace("{{ loot_table.col.where }}", col_html if i == 0 else "")
            table_rows.append(row_html)

    html = html.replace("{{ loot_table.rows }}", "\n".join(table_rows))
    return html


def render_feature(feature: Feature, index: int, count: int) -> str:
    html = context.templates["feature"]

    is_last = index == count - 1
    html = html.replace(
        "{{ accordion.heading }}",
        "border" + ("" if is_last else " border-b-0 ") + (" rounded-t-xl" if index == 0 else ""),
    )
    html = html.replace("{{ accordion.body }}", "border" + (" border-t-0" if is_last else " border-b-0"))

    recipes, loot = [], {}
    if "itemlike" in feature.metadata:
        recipes = [render_recipe(feature, r) for r in get_item_data(feature.metadata["itemlike"] + ".recipes").values()]
        loot = get_item_data(feature.metadata["itemlike"] + ".loot")
    html = (
        html.replace("{{ feature.recipes }}", "\n".join(recipes))
        if recipes
        else remove_optional(html, "{{ feature.recipes }}")
    )
    html = (
        html.replace("{{ feature.loot }}", render_loot_table(loot))
        if loot
        else remove_optional(html, "{{ feature.loot }}")
    )

    if "icon_overlay" in feature.metadata:
        feature.metadata["icon_overlay"] = item_to_icon(feature.metadata["icon_overlay"])
    else:
        html = remove_optional(html, "{{ feature.metadata.icon_overlay }}")

    for k, v in feature.metadata.items():
        html = html.replace(f"{{{{ feature.metadata.{k} }}}}", escape(str(v), quote=True))

    html = html.replace("{{ feature.html_content }}", feature.html_content)
    module_badge = context.templates["badge"].replace(
        "{{ color }}",
        context.content_settings["badge"]["color"][feature.metadata["module"]],
    )
    module_badge = module_badge.replace("{{ text }}", feature.metadata["module"].removeprefix("vane-"))
    html = html.replace("{{ feature.badge }}", module_badge)
    return html


def render_category_content(category: dict[str, Any]) -> str:
    # Pre-render features
    features_html = []
    fs = context.features[category["id"]]
    for i, f in enumerate(fs):
        print(f"Rendering feature {f.loaded_from}")
        features_html.append(render_feature(f, i, len(fs)))

    html = context.templates["category"]
    html = replace_category_variables(html, category)
    html = html.replace("{{ features }}", "\n".join(features_html))
    return html


class Links(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids = set()
        self.urls = []
        self.recipe_coordinates = set()

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if "id" in attrs:
            if attrs["id"] in self.ids:
                raise ValueError(f"Duplicate HTML id: {attrs['id']}")
            self.ids.add(attrs["id"])
        classes = attrs.get("class", "").split()
        if tag == "img" and "absolute" in classes and "mctooltip" in classes:
            coordinates = [re.fullmatch(r"(left|top|w|h)-\[([0-9.]+%)\]", c) for c in classes]
            coordinates = {(m[1], m[2]) for m in coordinates if m}
            if {prop for prop, _ in coordinates} != {"left", "top", "w", "h"}:
                raise ValueError("Recipe icons need valid percentage coordinates for Tailwind")
            self.recipe_coordinates.update(coordinates)
        for name in ("src", "href"):
            if name in attrs:
                self.urls.append(attrs[name])


def validate_site(output: Path) -> None:
    text = (output / "index.html").read_text(encoding="utf-8")
    if "{{" in text or "}}" in text:
        raise ValueError("Unexpanded template in generated site")
    links = Links()
    links.feed(text)
    root = output.resolve()
    css_file = output / "css/style.css"
    css = css_file.read_text(encoding="utf-8") if css_file.exists() else ""
    if css.strip():
        for prop, value in links.recipe_coordinates:
            prop = {"w": "width", "h": "height"}.get(prop, prop)
            if not re.search(rf"{prop}\s*:\s*{re.escape(value)}(?=[;}}])", css):
                raise ValueError(f"Missing compiled recipe CSS: {prop}: {value}")
    for url in links.urls:
        parts = urlsplit(url)
        if parts.scheme or parts.netloc:
            continue
        if not parts.path:
            if parts.fragment and unquote(parts.fragment) not in links.ids:
                raise ValueError(f"Broken anchor: {url}")
            continue
        path = (root / unquote(parts.path)).resolve()
        if not path.is_relative_to(root) or not path.is_file():
            raise ValueError(f"Broken local URL: {url}")


def build(output: Path) -> None:
    global context
    context = Context(
        templates={p.stem: p.read_text(encoding="utf-8").strip() for p in sorted((ROOT / "templates").glob("*.html"))},
        content_settings=tomllib.loads((ROOT / "content.toml").read_text(encoding="utf-8")),
        icons=json.loads((ROOT / "data/icons.json").read_text(encoding="utf-8")),
        items=json.loads((ROOT / "data/items.json").read_text(encoding="utf-8")),
    )
    used = []
    for category in context.content_settings["categories"]:
        used.extend(category["content"])
        context.features[category["id"]] = [
            load_feature_markdown(
                ROOT / "content" / name,
                "feature-" + name.removesuffix(".md").replace("/", "--"),
            )
            for name in category["content"]
        ]
    available = {p.relative_to(ROOT / "content").as_posix() for p in (ROOT / "content").rglob("*.md")}
    if len(set(used)) != len(used) or set(used) != available:
        raise ValueError(f"Content catalog is incomplete or duplicated: {available ^ set(used)}")
    html = context.templates["index"].replace(
        "{{ each_category_content }}",
        "\n".join(render_category_content(c) for c in context.content_settings["categories"]),
    )
    output.mkdir(parents=True, exist_ok=True)
    for name in STATIC_PATHS:
        source = ROOT / name
        if source.is_dir():
            shutil.copytree(source, output / name, dirs_exist_ok=True)
        else:
            shutil.copyfile(source, output / name)
    (output / "index.html").write_text(html + "\n", encoding="utf-8")
    # CSS is compiled separately by Tailwind. Validate everything else immediately.
    css = output / "css/style.css"
    had_css = css.exists()
    if not had_css:
        css.touch()
    try:
        validate_site(output)
    finally:
        if not had_css:
            css.unlink()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("-o", "--output-dir", type=Path, default=ROOT / "build")
    parser.add_argument("--resource-packs", type=Path)
    args = parser.parse_args()
    build(args.output_dir)
    if args.resource_packs:
        shutil.copytree(args.resource_packs, args.output_dir / "resourcepacks", dirs_exist_ok=True)


if __name__ == "__main__":
    main()
