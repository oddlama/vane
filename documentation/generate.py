#!/usr/bin/env python3

import argparse
import markdown
import os
import shutil
import toml
import zipfile
from dataclasses import dataclass, field
from glob import glob
from pathlib import Path
from typing import Any


@dataclass
class Feature:
    metadata: dict[str, Any]
    html_content: str

@dataclass
class Context:
    build_path: Path = Path("build")
    assets_path: Path = Path("build/assets")
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

    if ":" in metadata["icon"]:
        namespace, key = metadata["icon"].split(":", maxsplit=1)
        if namespace == "minecraft":
            metadata["icon"] = "minecraft/" + key
            context.required_minecraft_assets.add(metadata["icon"])
        elif namespace.startswith("vane-"):
            metadata["icon"] = f"{namespace}/{key}"
            context.required_project_assets.add((namespace, key))

    return Feature(metadata=metadata,
                   html_content=markdown.markdown(content))

def replace_category_variables(s: str, category: dict[str, Any]):
    s = s.replace("{{ category.id }}", category["id"])
    s = s.replace("{{ category.title }}", category["title"])
    return s

def replace_feature_variables(s: str, feature: Feature):
    for k,v in feature.metadata.items():
        s = s.replace(f"{{{{ feature.metadata.{k} }}}}", v)
    s = s.replace("{{ feature.html_content }}", feature.html_content)
    s = s.replace("{{ feature.icon }}", f"assets/{feature.metadata['icon']}")
    module_badge = context.templates["module-badge"].replace("{{ text }}", feature.metadata["module"])
    s = s.replace("{{ feature.badge }}", module_badge)
    return s

def render_category_index(category: dict[str, Any]) -> str:
    # Pre-render features
    features_html = []
    for f in context.features[category["id"]]:
        features_html.append(replace_feature_variables(context.templates["feature-index"], f))

    html = context.templates["category-index"]
    html = replace_category_variables(html, category)
    html = html.replace("{{ features }}", "\n".join(features_html))
    return html

def render_category_content(category: dict[str, Any]) -> str:
    # Pre-render features
    features_html = []
    fs = context.features[category["id"]]
    for i,f in enumerate(fs):
        f_html = context.templates["feature-content"]
        f_html = replace_feature_variables(f_html, f)
        is_last = i == len(fs) - 1
        f_html = f_html.replace("{{ accordion.heading }}", "border" + (""            if is_last else " border-b-0 ") + (" rounded-t-xl" if i == 0 else ""))
        f_html = f_html.replace("{{ accordion.body }}",    "border" + (" border-t-0" if is_last else " border-b-0"))
        features_html.append(f_html)

    html = context.templates["category-content"]
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

    print(f"Rendering index.html")
    index_content = context.templates["index"]
    # index_content = index_content.replace("{{ each_category_index }}", "\n".join(
    #     render_category_index(c) for c in context.content_settings["categories"]
    # ))
    # HINT: disabled for now. Didn't add much value but added clutter.
    index_content = index_content.replace("{{ each_category_index }}", "")
    index_content = index_content.replace("{{ each_category_content }}", "\n".join(
        render_category_content(c) for c in context.content_settings["categories"]
    ))

    with open(context.build_path / "index.html", "w") as f:
        f.write(index_content)

def collect_assets(client_jar: str) -> None:
    print(f"Collecting {len(context.required_minecraft_assets)} required assets from client jar...")
    with zipfile.ZipFile(client_jar) as zf:
        for asset in context.required_minecraft_assets:
            out = context.assets_path / asset
            out.parent.mkdir(parents=True, exist_ok=True)
            with open(out, "wb") as f:
                f.write(zf.read("assets/" + asset))

    print(f"Collecting {len(context.required_project_assets)} required assets from this project...")
    for namespace, key in context.required_project_assets:
        out = context.assets_path / namespace / key
        out.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(Path("..") / namespace / "src/main/resources" / key, out)

def main():
    parser = argparse.ArgumentParser(description="Generates the documentation page.")
    parser.add_argument('--client-jar', required=True, type=str,
            help="Specifies a minecraft client jar file from which required assets will be extracted.")
    parser.add_argument('-o', '--output-dir', dest='output_dir', default="build", type=str,
            help="Specifies the output directory for the documentation. (default: 'build')")
    args = parser.parse_args()

    context.build_path = Path(args.output_dir)
    context.build_path.mkdir(parents=True, exist_ok=True)

    context.assets_path = context.build_path / "assets"
    context.assets_path.mkdir(parents=True, exist_ok=True)

    context.content_settings = toml.load("content.toml")
    assert "categories" in context.content_settings
    context.categories = { c["id"]: c for c in context.content_settings["categories"] }
    context.templates = load_templates()

    generate_docs()
    collect_assets(args.client_jar)

if __name__ == "__main__":
    main()
