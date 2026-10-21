from pathlib import Path
import sys

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
import generate


def test_site_build_is_deterministic(tmp_path):
    first, second = tmp_path / "first", tmp_path / "second"
    generate.build(first)
    generate.build(second)
    assert (first / "index.html").read_bytes() == (second / "index.html").read_bytes()
    assert "Papyrus Scroll" in (first / "index.html").read_text()
    assert "minecraft:echo_shard" in (first / "index.html").read_text()
    assert "{{" not in (first / "index.html").read_text()
    assert (first / "assets").is_dir()
    assert (first / "css/Minecraftia.woff").is_file()
    for source in ("content", "data", "templates", "tests", "generate.py", "build.sh", "README.md"):
        assert not (first / source).exists()


def test_frontmatter_allows_blank_line_before_separator(tmp_path):
    generate.build(tmp_path / "site")
    page = tmp_path / "feature.md"
    page.write_text('```toml\ntitle = "Example"\nicon = "minecraft:paper"\n```\n\n---\nHello\n')
    feature = generate.load_feature_markdown(page, "example")
    assert feature.metadata["slug"] == "example"
    assert feature.html_content == "<p>Hello</p>"


def test_inline_icons_survive_markdown_tables(tmp_path):
    generate.build(tmp_path / "site")
    page = tmp_path / "table.md"
    page.write_text("""```toml
title = "Table"
icon = "minecraft:paper"
```
---
| Material |
| --- |
| {{ item:minecraft:crying_obsidian }} |
""")
    feature = generate.load_feature_markdown(page, "table")
    links = generate.Links()
    links.feed(feature.html_content)
    assert "<table>" in feature.html_content
    assert generate.item_to_icon("minecraft:crying_obsidian") in links.urls


def test_missing_icon_is_an_error():
    with pytest.raises(ValueError, match="No documented icon"):
        generate.item_to_icon("minecraft:not_a_real_item")


@pytest.mark.parametrize("path", ["../generate.py", "assets/../generate.py", "generate.py", "templates/style.css"])
def test_assets_cannot_escape_static_paths(path):
    with pytest.raises(ValueError, match="unsafe"):
        generate.asset_path(path)


def test_missing_compiled_recipe_css_is_an_error(tmp_path):
    (tmp_path / "css").mkdir()
    (tmp_path / "css/style.css").write_text("body { color: black; }")
    (tmp_path / "index.html").write_text('<img class="absolute mctooltip left-[1%] top-[2%] w-[3%] h-[4%]">')
    with pytest.raises(ValueError, match="Missing compiled recipe CSS"):
        generate.validate_site(tmp_path)


def test_broken_local_links_fail_the_build(tmp_path):
    (tmp_path / "index.html").write_text('<img src="missing.png">')
    with pytest.raises(ValueError, match="Broken local URL"):
        generate.validate_site(tmp_path)


def test_broken_anchors_and_duplicate_ids_are_errors(tmp_path):
    for html, message in [
        ('<a href="#missing">link</a>', "Broken anchor"),
        ('<p id="same"></p><p id="same"></p>', "Duplicate HTML id"),
    ]:
        (tmp_path / "index.html").write_text(html)
        with pytest.raises(ValueError, match=message):
            generate.validate_site(tmp_path)
