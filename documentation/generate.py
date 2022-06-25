#!/usr/bin/env python3

import argparse
import markdown
import os
import shutil
import toml
import yaml
import zipfile
from dataclasses import dataclass, field
from functools import reduce
from glob import glob
from pathlib import Path
from typing import Any


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
    assert "feature" in metadata
    metadata = metadata["feature"]
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
        else:
            metadata["icon"] = item_to_icon(metadata["icon"])
    else:
        if "itemlike" not in metadata:
            raise ValueError("metadata contains no icon definition. This is only possible if 'itemlike' is set to determine the icon from the recipe.")

    return Feature(loaded_from=markdown_file,
                   metadata=metadata,
                   html_content=markdown.markdown(content))

def replace_category_variables(s: str, category: dict[str, Any]):
    s = s.replace("{{ category.id }}", category["id"])
    s = s.replace("{{ category.title }}", category["title"])
    return s

def deep_get(dictionary, keys):
    return reduce(lambda d, key: d[key], keys.split("."), dictionary)

def get_from_config(resource_key: str) -> dict[str, Any]:
    namespace, key = resource_key.split(":", maxsplit=1)
    with open(context.plugins_dir / namespace / "config.yml") as f:
        config = yaml.safe_load(f)
    return deep_get(config, key)

def item_to_icon(item: str) -> str:
    resource_key = item.split("{")[0]
    if not resource_key.startswith("#"):
        resource_key = resource_key.split("#")[0]
    namespace, key = resource_key.split(":", maxsplit=1)
    if namespace == "minecraft":
        # Some items require manual rewrites
        if key == "compass":
            key = "compass_19"
        elif key == "clock":
            key = "clock_02"
        icon = f"assets/minecraft/textures/item/{key}.png"
        context.required_minecraft_assets.add(icon)
        return icon
    elif namespace.startswith("vane"):
        namespace = namespace.replace("_", "-")
        key = f"items/{key}.png"
        icon = f"{namespace}/{key}"
        context.required_project_assets.add((namespace, key))
        return "assets/" + icon
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
        print("TODO: smithing recipe")
        return ""
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
    loot_badge = context.templates["badge"]
    col_template = context.templates["loot-table-col-where"]
    row_template = context.templates["loot-table-row"]

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

        col_html = col_template
        col_html = col_html.replace("{{ loot_table.n_rows }}", str(len(table["items"])))
        col_html = col_html.replace("{{ loot_table.where.rows }}", where_rows)

        for i,item in enumerate(table["items"]):
            row_html = row_template
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
        html = html.replace("{{ feature.recipes }}", "\n".join(recipes))

        loot = get_from_config(feature.metadata["itemlike"] + ".loot")
        html = html.replace("{{ feature.loot }}", render_loot_table(loot) if len(loot) > 0 else "")
    else:
        html = html.replace("{{ feature.recipes }}", "")
        html = html.replace("{{ feature.loot }}", "")

    if "icon" not in feature.metadata:
        print(f"[1;33mwarning:[m could not infer icon")

    if "icon_overlay" in feature.metadata:
        feature.metadata["icon_overlay"] = item_to_icon(feature.metadata["icon_overlay"])
    else:
        html = remove_lines_containing(html, "{{ feature.metadata.icon_overlay }}")

    for k,v in feature.metadata.items():
        html = html.replace(f"{{{{ feature.metadata.{k} }}}}", v)

    html = html.replace("{{ feature.html_content }}", feature.html_content)
    module_badge = context.templates["badge"].replace("{{ text }}", feature.metadata["module"])
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

def collect_assets(client_jar: str) -> None:
    print(f"Collecting {len(context.required_minecraft_assets)} required assets from client jar...")
    with zipfile.ZipFile(client_jar) as zf:
        for asset in context.required_minecraft_assets:
            out = context.assets_path / asset.removeprefix("assets/")
            out.parent.mkdir(parents=True, exist_ok=True)
            with open(out, "wb") as f:
                try:
                    f.write(zf.read(asset))
                except KeyError:
                    print(f"[1;33mwarning:[m missing asset: {asset}")

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

    generate_docs()
    collect_assets(args.client_jar)

if __name__ == "__main__":
    main()
