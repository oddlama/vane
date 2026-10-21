import json
from pathlib import Path
import sys
import zipfile

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))
import publish
import normalize_jars
import release
import testserver


def artifacts(tmp_path):
    plugins, wheel, paper = [tmp_path / name for name in ("plugins", "wheel", "paper")]
    for directory in (plugins, wheel, paper):
        directory.mkdir()
    for module in release.PAPER_MODULES + ("velocity", "geyser-extension"):
        (plugins / f"vane-{module}-1.2.3.jar").write_bytes(module.encode())
    (plugins / "barmc-1.2.3.zip").write_bytes(b"bridge")
    (wheel / "barmc-1.2.3-py3-none-any.whl").write_bytes(b"wheel")
    (paper / "paper.jar").write_bytes(b"paper")
    return plugins, wheel, paper


def test_release_is_deterministic_and_paper_only(tmp_path):
    plugins, wheel, _ = artifacts(tmp_path)
    first, second = tmp_path / "one", tmp_path / "two"
    metadata = {"version": "1.2.3", "revision": None}
    release.assemble(plugins, wheel, first, metadata)
    release.assemble(plugins, wheel, second, metadata)
    for file in first.iterdir():
        assert file.read_bytes() == (second / file.name).read_bytes()
    with zipfile.ZipFile(first / "all-plugins.zip") as archive:
        assert len(archive.namelist()) == 9
        assert not any("velocity" in n or "geyser" in n for n in archive.namelist())
    assert publish.verify(first)["version"] == "1.2.3"
    (first / "all-plugins.zip").write_bytes(b"tampered")
    with pytest.raises(ValueError, match="checksum"):
        publish.verify(first)


def test_publishing_defaults_to_an_offline_dry_run(tmp_path, monkeypatch, capsys):
    plugins, wheel, _ = artifacts(tmp_path)
    assets = tmp_path / "release"
    release.assemble(plugins, wheel, assets, {"version": "1.2.3", "revision": None})
    monkeypatch.setenv("MODRINTH_TOKEN", "must-not-appear-in-the-plan")

    def forbidden(*args, **kwargs):
        raise AssertionError("A dry run must never invoke a publisher")

    monkeypatch.setattr(publish, "github", forbidden)
    monkeypatch.setattr(publish, "modrinth", forbidden)
    for destination in ("github", "modrinth"):
        monkeypatch.setattr(sys, "argv", ["publish", destination, "--assets", str(assets)])
        publish.main()
        output = capsys.readouterr().out
        assert json.loads(output)["dry_run"] is True
        assert "must-not-appear-in-the-plan" not in output


def test_remote_tag_must_match_the_artifact_source(monkeypatch):
    monkeypatch.setattr(publish, "gh", lambda *args: "different-revision")
    with pytest.raises(ValueError, match="does not identify"):
        publish.check_tag({"version": "1.2.3", "revision": "expected-revision", "repository": "oddlama/vane"})


def test_runtime_jar_normalization_preserves_content_and_manifest(tmp_path):
    contents = {"z.class": b"z", "META-INF/MANIFEST.MF": b"Manifest-Version: 1.0\n\n", "a.class": b"a"}
    first, second = tmp_path / "one.jar", tmp_path / "two.jar"
    for path, items, year in [(first, list(contents.items()), 2025), (second, list(reversed(contents.items())), 2026)]:
        with zipfile.ZipFile(path, "w") as archive:
            for name, data in items:
                archive.writestr(zipfile.ZipInfo(name, (year, 1, 1, 0, 0, 0)), data)
    normalized_first, normalized_second = tmp_path / "one-normalized.jar", tmp_path / "two-normalized.jar"
    normalize_jars.normalize_archive(first, normalized_first)
    normalize_jars.normalize_archive(second, normalized_second)
    assert normalized_first.read_bytes() == normalized_second.read_bytes()
    with zipfile.ZipFile(normalized_first) as archive:
        assert archive.namelist()[0] == "META-INF/MANIFEST.MF"
        assert {name: archive.read(name) for name in archive.namelist()} == contents


def test_dirty_sources_cannot_be_published():
    with pytest.raises(ValueError, match="dirty"):
        publish.check_tag({"revision": None})


def test_modrinth_layout_matches_existing_project():
    variants = publish.modrinth_variants({"version": "1.2.3"})
    assert variants["main"]["files"][0] == "vane-core-1.2.3.jar"
    assert variants["geyser"]["loaders"] == ["geyser"]
    assert variants["geyser"]["number"] == "v1.2.3-Geyser"


def test_testserver_refuses_unmanaged_directory(tmp_path):
    plugins, _, paper = artifacts(tmp_path)
    state = tmp_path / "production"
    state.mkdir()
    (state / "world").write_text("keep me")
    with pytest.raises(ValueError, match="unmanaged"):
        testserver.prepare(state, plugins, paper, "1.2.3", 25565)
    assert (state / "world").read_text() == "keep me"
    assert not (state / "eula.txt").exists()


def test_testserver_restart_preserves_worlds_and_other_plugins(tmp_path):
    plugins, _, paper = artifacts(tmp_path)
    state = tmp_path / "dev"
    testserver.prepare(state, plugins, paper, "1.2.3", 25565)
    (state / "world").write_text("keep world")
    (state / "plugins/other.jar").write_text("keep plugin")
    testserver.prepare(state, plugins, paper, "1.2.3", 25565)
    assert (state / "world").read_text() == "keep world"
    assert (state / "plugins/other.jar").read_text() == "keep plugin"
    assert "server-ip=127.0.0.1" in (state / "server.properties").read_text()
    assert not (state / "plugins/vane-velocity-1.2.3.jar").exists()


def test_testserver_rejects_runtime_upgrade_in_existing_state(tmp_path):
    plugins, _, paper = artifacts(tmp_path)
    state = tmp_path / "dev"
    testserver.prepare(state, plugins, paper, "1.2.3", 25565)
    other = tmp_path / "other-paper"
    other.mkdir()
    with pytest.raises(ValueError, match="runtime changed"):
        testserver.prepare(state, plugins, other, "1.2.3", 25565)


def test_eula_is_required_before_build_or_state_creation(tmp_path, monkeypatch):
    monkeypatch.delenv("VANE_ACCEPT_EULA", raising=False)
    state = tmp_path / "untouched"
    monkeypatch.setattr(
        sys,
        "argv",
        [
            "testserver",
            "--plugins",
            str(tmp_path),
            "--paper",
            str(tmp_path),
            "--java",
            "/not/java",
            "--version",
            "1.2.3",
            "--state-dir",
            str(state),
            "--build",
        ],
    )
    with pytest.raises(SystemExit) as error:
        testserver.main()
    assert error.value.code == 2
    assert not state.exists()
