#!/usr/bin/env python3

import argparse
import json
import markdown
import os
import re
import shutil
import subprocess
import tempfile
import toml
import yaml
import zipfile
from dataclasses import dataclass, field
from functools import reduce
from glob import glob
from pathlib import Path
from typing import Any

markdown_item_pattern = re.compile(r'{{ item:(\S*) }}')

@dataclass
class Feature:
    loaded_from: str
    metadata: dict[str, Any]
    html_content: str

@dataclass
class Context:
    build_path: Path = Path("build")
    assets_path: Path = Path("build/assets")
    plugins_dir: Path = Path("plugins")
    client_jar: zipfile.ZipFile = None # type: ignore
    loaded_minecraft_asset_icons: dict[str, str] = field(default_factory=dict)
    content_settings: dict[str, Any] = field(default_factory=dict)
    features: dict[str, list[Feature]] = field(default_factory=dict)
    categories: dict[str, Any] = field(default_factory=dict)
    templates: dict[str, str] = field(default_factory=dict)
    required_minecraft_assets: set[str] = field(default_factory=set)
    required_project_assets: set[tuple[str, str]] = field(default_factory=set)

context = Context()

def load_templates() -> dict[str, str]:
    templates = {}
    for i in glob("templates/*.html"):
        with open(i, "r") as f:
            templates[os.path.basename(i).removesuffix(".html")] = f.read()
    return templates

def item_to_inline_icon(resource_key: str) -> str:
    html = context.templates["inline-icon"].strip()
    html = html.replace("{{ icon }}", item_to_icon(resource_key))
    html = html.replace("{{ name }}", resource_key)
    return html

def load_feature_markdown(markdown_file: Path, default_slug: str) -> Feature:
    with open(markdown_file, "r") as f:
        try:
            metadata, content = f.read().split("```\n---\n", maxsplit=1)
            metadata = metadata.removeprefix("```toml")
            content = content.strip()
        except ValueError:
            print("Missing or invalid metadata section.")
            exit(1)

    metadata = toml.loads(metadata)
    if "slug" not in metadata:
        metadata["slug"] = default_slug

    if "icon" in metadata:
        if ":" in metadata["icon"] and metadata["icon"].endswith(".png"):
            namespace, key = metadata["icon"].split(":", maxsplit=1)
            if namespace == "minecraft":
                metadata["icon"] = "assets/minecraft/" + key
                context.required_minecraft_assets.add(metadata["icon"])
            elif namespace.startswith("vane-"):
                metadata["icon"] = f"assets/{namespace}/{key}"
                context.required_project_assets.add((namespace, key))
                print("a", (namespace, key))
        else:
            metadata["icon"] = item_to_icon(metadata["icon"])
    else:
        if "itemlike" not in metadata:
            raise ValueError("metadata contains no icon definition. This is only possible if 'itemlike' is set to determine the icon from the recipe.")

    content = markdown_item_pattern.sub(lambda match: item_to_inline_icon(match.group(1)), content)
    return Feature(loaded_from=str(markdown_file),
                   metadata=metadata,
                   html_content=markdown.markdown(content, extensions=['tables']))

def replace_category_variables(s: str, category: dict[str, Any]):
    s = s.replace("{{ category.id }}", category["id"])
    s = s.replace("{{ category.title }}", category["title"])
    return s

def deep_get(dictionary, keys):
    return reduce(lambda d, key: d[key], keys.split("."), dictionary)

def get_from_config(resource_key: str) -> dict[str, Any]:
    try:
        namespace, key = resource_key.split(":", maxsplit=1)
        with open(context.plugins_dir / namespace / "config.yml") as f:
            config = yaml.safe_load(f)
        return deep_get(config, key)
    except:
        print(f"Error while trying to get {resource_key} from config")
        raise

def collect_jar_asset(asset: str) -> None:
    out = context.assets_path / asset.removeprefix("assets/")
    out.parent.mkdir(parents=True, exist_ok=True)
    with open(out, "wb") as f:
        try:
            f.write(context.client_jar.read(asset))
        except KeyError:
            print(f"[1;33mwarning:[m missing asset: {asset}")

def _render_block(texture_front: str, texture_side: str, texture_top: str, output: Path):
    size = 128
    if not output.exists():
        subprocess.run(["convert", "-size", f"{size}x{size}", "xc:transparent",
            "(", texture_top, "-interpolate", "Nearest", "-filter", "point", "-resize", "3200%",
                "-alpha", "set", "-virtual-pixel", "transparent", "+distort", "Perspective",
                    f"{int(0 * 512)},{int(0 * 512)} {int(150/300 * size)},{int(  0/300 * size)} \
                    {int(1 * 512)},{int(0 * 512)} {int(284/300 * size)},{int( 68/300 * size)} \
                    {int(0 * 512)},{int(1 * 512)} {int( 16/300 * size)},{int( 68/300 * size)} \
                    {int(1 * 512)},{int(1 * 512)} {int(150/300 * size)},{int(135/300 * size)}", ")",
            "(", texture_side, "-interpolate", "Nearest", "-filter", "point", "-resize", "3200%",
                "-alpha", "set", "-virtual-pixel", "transparent", "+distort", "Perspective",
                    f"{int(0 * 512)},{int(0 * 512)} {int( 16/300 * size)},{int( 68/300 * size)} \
                    {int(1 * 512)},{int(0 * 512)} {int(150/300 * size)},{int(135/300 * size)} \
                    {int(0 * 512)},{int(1 * 512)} {int( 16/300 * size)},{int(232/300 * size)} \
                    {int(1 * 512)},{int(1 * 512)} {int(150/300 * size)},{int(300/300 * size)}", ")",
            "(", texture_front, "-interpolate", "Nearest", "-filter", "point", "-resize", "3200%",
                "-alpha", "set", "-virtual-pixel", "transparent", "+distort", "Perspective",
                    f"{int(0 * 512)},{int(0 * 512)} {int(150/300 * size)},{int(135/300 * size)} \
                    {int(1 * 512)},{int(0 * 512)} {int(284/300 * size)},{int( 68/300 * size)} \
                    {int(0 * 512)},{int(1 * 512)} {int(150/300 * size)},{int(300/300 * size)} \
                    {int(1 * 512)},{int(1 * 512)} {int(284/300 * size)},{int(232/300 * size)}", ")",
            "-background", "transparent", "-compose", "plus", "-layers", "flatten", "+repage", str(output)
        ], check=True)

def render_cube_all(key: str, model: dict[str, Any]) -> str:
    print(f"Rendering cube_all {key}...")
    texture = model['textures']['all'].removeprefix('minecraft:')

    with tempfile.NamedTemporaryFile() as ftmp:
        asset = f"assets/minecraft/textures/{texture}.png"
        ftmp.write(context.client_jar.read(asset))
        ftmp.flush()

        icon = f"assets/minecraft/blocks/{key}.png"
        out = context.assets_path / icon.removeprefix("assets/")
        out.parent.mkdir(parents=True, exist_ok=True)
        _render_block(ftmp.name, ftmp.name, ftmp.name, out)

    return icon

def render_orientable(key: str, model: dict[str, Any]) -> str:
    print(f"Rendering orientable {key}...")
    texture_front = model['textures']['front'].removeprefix('minecraft:')
    texture_side = model['textures']['side'].removeprefix('minecraft:')
    texture_top = model['textures']['top'].removeprefix('minecraft:')

    with tempfile.NamedTemporaryFile() as tmp_front, tempfile.NamedTemporaryFile() as tmp_side, tempfile.NamedTemporaryFile() as tmp_top:
        tmp_front.write(context.client_jar.read(f"assets/minecraft/textures/{texture_front}.png"))
        tmp_front.flush()
        tmp_side.write(context.client_jar.read(f"assets/minecraft/textures/{texture_side}.png"))
        tmp_side.flush()
        tmp_top.write(context.client_jar.read(f"assets/minecraft/textures/{texture_top}.png"))
        tmp_top.flush()

        icon = f"assets/minecraft/blocks/{key}.png"
        out = context.assets_path / icon.removeprefix("assets/")
        out.parent.mkdir(parents=True, exist_ok=True)
        out.parent.mkdir(parents=True, exist_ok=True)
        _render_block(tmp_front.name, tmp_side.name, tmp_top.name, out)

    return icon

def minecraft_asset_icon(key: str) -> str:
    # Don't load again if already known
    if key in context.loaded_minecraft_asset_icons:
        return context.loaded_minecraft_asset_icons[key]

    icon = None
    item_model = None
    msg = ""
    try:
        item_model = json.loads(context.client_jar.read(f"assets/minecraft/models/item/{key}.json"))
    except KeyError:
        msg = f"unknown item minecraft:{key}"

    if item_model is not None:
        item_parent = item_model["parent"].removeprefix("minecraft:")
        if item_parent in ["item/generated", "item/handheld"]:
            texture = item_model["textures"]["layer0"].removeprefix('minecraft:')
            icon = f"assets/minecraft/textures/{texture}.png"
            collect_jar_asset(icon)
        elif item_parent.startswith("block/"):
            block = item_parent.removeprefix("block/")
            block_model = json.loads(context.client_jar.read(f"assets/minecraft/models/block/{block}.json"))
            block_parent = block_model['parent'].removeprefix("minecraft:")
            if block_parent == "block/cube_all":
                icon = render_cube_all(key, block_model)
            elif block_parent == "block/orientable":
                icon = render_orientable(key, block_model)
            else:
                msg = f"unknown block model type {block_parent} in item minecraft:{key}"
        else:
            msg = f"unknown item model type {item_parent} in item minecraft:{key}"

    if icon is None:
        icon = f"assets/minecraft_special/{key}.png"
        out = context.assets_path / icon.removeprefix("assets/")
        print(f"using {icon} for {msg}")
        if not out.exists():
            print(f"[1;33mwarning:[m missing asset: {icon}")

    context.loaded_minecraft_asset_icons[key] = icon
    return icon

def item_to_icon(item: str) -> str:
    resource_key = item.split("{")[0]
    if not resource_key.startswith("#"):
        resource_key = resource_key.split("#")[0]
    namespace, key = resource_key.split(":", maxsplit=1)

    # FIXME: contains hardcoded overrides
    if resource_key == "minecraft:leather_chestplate":
        return f"assets/minecraft_special/leather_chestplate.png"
    elif namespace == "minecraft":
        return minecraft_asset_icon(key)
    elif namespace.startswith("vane"):
        if resource_key == "vane_trifles:north_compass":
            context.required_project_assets.add(("vane-trifles", "items/north_compass_16.png"))
            return f"assets/vane-trifles/items/north_compass_16.png"
        else:
            namespace = namespace.replace("_", "-")
            key = f"items/{key}.png"
            icon = f"{namespace}/{key}"
            context.required_project_assets.add((namespace, key))
            return "assets/" + icon
    elif resource_key == "#minecraft:beds":
        return f"assets/minecraft_special/bed.png"
    elif resource_key == "#minecraft:shulker_boxes":
        return f"assets/minecraft_special/shulker_box.png"
    elif resource_key == "#minecraft:saplings":
        return minecraft_asset_icon("oak_sapling")
    else:
        print(f"[1;33mwarning:[m unknown icon for item: {item}")
    return f"assets/minecraft/textures/item/barrier.png"

def remove_lines_containing(where: str, what: str):
    return "".join(line for line in where.splitlines(keepends=True) if what not in line)

def render_recipe(feature: Feature, recipe: dict[str, Any]) -> str:
    if recipe["type"] == "shaped":
        html = context.templates["shaped-recipe"]

        shape = "".join([row.ljust(3) for row in (recipe["shape"] + 3 * [""])[:3]])
        ingredients = {str(k):v for k,v in recipe["ingredients"].items()}
        assert len(shape) == 9
        for i,c in enumerate(shape):
            tag = f"{{{{ recipe.ingredients.{i} }}}}"
            if c == " ":
                html = remove_lines_containing(html, tag)
            else:
                ingredient = ingredients[c]
                html = html.replace(tag, item_to_icon(ingredient))
                html = html.replace(f"{{{{ recipe.ingredients.{i}.name }}}}", ingredient)
    elif recipe["type"] == "shapeless":
        html = context.templates["shaped-recipe"] # Abuse the template for shapeless recipes

        for i,ingredient in enumerate(recipe["ingredients"] + (9 - len(recipe["ingredients"])) * [None]):
            tag = f"{{{{ recipe.ingredients.{i} }}}}"
            if ingredient is None:
                html = remove_lines_containing(html, tag)
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
            badge_html = badge_html + '</div>'
            where_rows.append(badge_html)
        where_rows = "\n".join(where_rows)

        col_html = context.templates["loot-table-col-where"]
        col_html = col_html.replace("{{ loot_table.n_rows }}", str(len(table["items"])))
        col_html = col_html.replace("{{ loot_table.where.rows }}", where_rows)

        for i,item in enumerate(table["items"]):
            row_html = context.templates["loot-table-row"]
            row_html = row_html.replace("{{ loot_table.row.item.name }}", item["item"])
            row_html = row_html.replace("{{ loot_table.row.item.icon }}", item_to_icon(item["item"]))
            row_html = row_html.replace("{{ loot_table.row.chance }}", f"{item['chance'] * 100.0:.2f}%")
            if item['amount_min'] == item['amount_max']:
                amount = str(item['amount_min'])
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
    html = html.replace("{{ accordion.heading }}", "border" + (""            if is_last else " border-b-0 ") + (" rounded-t-xl" if index == 0 else ""))
    html = html.replace("{{ accordion.body }}",    "border" + (" border-t-0" if is_last else " border-b-0"))

    if "itemlike" in feature.metadata:
        recipes = [render_recipe(feature, r) for r in get_from_config(feature.metadata["itemlike"] + ".recipes").values()]
        if len(recipes) > 0:
            html = html.replace("{{ feature.recipes }}", "\n".join(recipes))
        else:
            html = remove_lines_containing(html, "{{ feature.recipes }}")

        loot = get_from_config(feature.metadata["itemlike"] + ".loot")
        if len(loot) > 0:
            html = html.replace("{{ feature.loot }}", render_loot_table(loot))
        else:
            html = remove_lines_containing(html, "{{ feature.loot }}")
    else:
        html = remove_lines_containing(html, "{{ feature.recipes }}")
        html = remove_lines_containing(html, "{{ feature.loot }}")

    if "icon" not in feature.metadata:
        print(f"[1;33mwarning:[m could not infer icon")

    if "icon_overlay" in feature.metadata:
        feature.metadata["icon_overlay"] = item_to_icon(feature.metadata["icon_overlay"])
    else:
        html = remove_lines_containing(html, "{{ feature.metadata.icon_overlay }}")

    for k,v in feature.metadata.items():
        html = html.replace(f"{{{{ feature.metadata.{k} }}}}", v)

    html = html.replace("{{ feature.html_content }}", feature.html_content)
    module_badge = context.templates["badge"].replace("{{ color }}", context.content_settings["badge"]["color"][feature.metadata["module"]])
    module_badge = module_badge.replace("{{ text }}", feature.metadata["module"].removeprefix("vane-"))
    html = html.replace("{{ feature.badge }}", module_badge)
    return html

def render_category_content(category: dict[str, Any]) -> str:
    # Pre-render features
    features_html = []
    fs = context.features[category["id"]]
    for i,f in enumerate(fs):
        print(f"Rendering feature {f.loaded_from}")
        features_html.append(render_feature(f, i, len(fs)))

    html = context.templates["category"]
    html = replace_category_variables(html, category)
    html = html.replace("{{ features }}", "\n".join(features_html))
    return html

def generate_docs() -> None:
    # Load features
    for c in context.content_settings["categories"]:
        fs = []
        for i in c["content"]:
            print(f"Processing {i}")
            fs.append(load_feature_markdown(Path("content") / i,
            default_slug="feature-" + i.removeprefix("content/").removesuffix(".md").replace("/", "--")))
        context.features[c["id"]] = fs

    index_content = context.templates["index"]
    index_content = index_content.replace("{{ each_category_content }}", "\n".join(
        render_category_content(c) for c in context.content_settings["categories"]
    ))

    print(f"Writing index.html")
    with open(context.build_path / "index.html", "w") as f:
        f.write(index_content)

def collect_assets() -> None:
    print(f"Collecting {len(context.required_minecraft_assets)} required assets from client jar...")
    for asset in context.required_minecraft_assets:
        collect_jar_asset(asset)

    print(f"Collecting {len(context.required_project_assets)} required assets from this project...")
    for namespace, key in context.required_project_assets:
        out = context.assets_path / namespace / key
        out.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(Path("..") / namespace / "src/main/resources" / key, out)

def main():
    parser = argparse.ArgumentParser(description="Generates the documentation page.")
    parser.add_argument('--client-jar', required=True, type=str,
            help="Specifies a minecraft client jar file from which required assets will be extracted.")
    parser.add_argument('--plugins-dir', required=True, type=str,
            help="Specifies a plugins/ directory from where vane's generated config can be read (for recipes and loot).")
    parser.add_argument('-o', '--output-dir', dest='output_dir', default="build", type=str,
            help="Specifies the output directory for the documentation. (default: 'build')")
    args = parser.parse_args()

    context.build_path = Path(args.output_dir)
    context.build_path.mkdir(parents=True, exist_ok=True)

    context.assets_path = context.build_path / "assets"
    context.assets_path.mkdir(parents=True, exist_ok=True)

    context.plugins_dir = Path(args.plugins_dir)

    context.content_settings = toml.load("content.toml")
    assert "categories" in context.content_settings
    context.categories = { c["id"]: c for c in context.content_settings["categories"] }
    context.templates = load_templates()

    with zipfile.ZipFile(args.client_jar) as client_jar:
        context.client_jar = client_jar
        generate_docs()
        collect_assets()

    # Ensure that all content documents are included as a safety check
    used = set(f for cat in context.content_settings["categories"] for f in cat["content"])
    available = set(f.removeprefix("content/") for f in glob("content/**/*.md", recursive=True))
    missing = available - used
    if len(missing) > 0:
        print(f"[1;33mwarning:[m unused content templates: {', '.join(missing)}")

if __name__ == "__main__":
    main()
