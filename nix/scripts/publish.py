"""Publish already-built assets. Dry-run unless --confirm is explicitly supplied."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
from contextlib import ExitStack

import requests


def verify(assets):
    manifest = json.loads((assets / "manifest.json").read_text())
    for name, expected in manifest["artifacts"].items():
        if Path(name).name != name:
            raise ValueError("Unsafe artifact name")
        path = assets / name
        if path.is_symlink() or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
            raise ValueError(f"Artifact checksum mismatch: {name}")
    return manifest


def modrinth_variants(manifest):
    version = manifest["version"]
    return {
        "main": {
            "number": f"v{version}",
            "loaders": ["paper", "purpur", "velocity"],
            "files": [
                f"vane-core-{version}.jar",
                "all-plugins.zip",
                f"vane-velocity-{version}.jar",
            ],
        },
        "geyser": {
            "number": f"v{version}-Geyser",
            "loaders": ["geyser"],
            "files": [f"vane-geyser-extension-{version}.jar"],
        },
    }


def gh(*args):
    return subprocess.check_output(["gh", *args], text=True).strip()


def check_tag(manifest):
    revision = manifest.get("revision")
    if not revision:
        raise ValueError("Refusing to publish a dirty or unversioned source tree; build the clean release tag")
    tag = "v" + manifest["version"]
    remote = gh("api", f"repos/{manifest['repository']}/commits/{tag}", "--jq", ".sha")
    if remote != revision:
        raise ValueError(f"Remote {tag} does not identify the source revision in the manifest")
    return tag


def github(assets, manifest):
    tag = check_tag(manifest)
    repo = manifest["repository"]
    result = subprocess.run(
        ["gh", "release", "view", tag, "--repo", repo, "--json", "isDraft"],
        text=True,
        capture_output=True,
    )
    if result.returncode == 0:
        if not json.loads(result.stdout)["isDraft"]:
            raise ValueError("Refusing to replace assets on an already published GitHub release")
    else:
        subprocess.run(
            [
                "gh",
                "release",
                "create",
                tag,
                "--repo",
                repo,
                "--verify-tag",
                "--draft",
                "--generate-notes",
                "--title",
                tag,
                *(["--prerelease"] if "-" in manifest["version"] else []),
            ],
            check=True,
        )
    files = list(manifest["artifacts"]) + ["SHA256SUMS", "manifest.json"]
    # Deliberately no --clobber: even draft asset replacement should be a conscious manual action.
    subprocess.run(
        [
            "gh",
            "release",
            "upload",
            tag,
            "--repo",
            repo,
            *[str(assets / name) for name in files],
        ],
        check=True,
    )


def modrinth(assets, manifest, variant):
    token = os.environ.get("MODRINTH_TOKEN")
    if not token:
        raise ValueError("MODRINTH_TOKEN must be supplied at runtime, never through Nix")
    tag = check_tag(manifest)
    release = json.loads(
        gh(
            "release",
            "view",
            tag,
            "--repo",
            manifest["repository"],
            "--json",
            "isDraft,body",
        )
    )
    if release["isDraft"]:
        raise ValueError("Review and publish the GitHub release before publishing to Modrinth")
    variants = modrinth_variants(manifest)
    selected = list(variants.values()) if variant == "all" else [variants[variant]]
    session = requests.Session()
    session.headers.update(
        {
            "Authorization": token,
            "User-Agent": "oddlama/vane Nix release tooling (https://github.com/oddlama/vane)",
        }
    )
    response = session.get(
        f"https://api.modrinth.com/v2/project/{manifest['modrinth_project']}/version",
        timeout=60,
    )
    response.raise_for_status()
    existing = {v["version_number"] for v in response.json()}
    for spec in selected:
        if spec["number"] in existing:
            raise ValueError(f"Modrinth version already exists: {spec['number']}; no overwrite attempted")
    for spec in selected:
        parts = [f"file{i}" for i in range(len(spec["files"]))]
        data = {
            "project_id": manifest["modrinth_project"],
            "version_number": spec["number"],
            "name": spec["number"],
            "changelog": release["body"],
            "dependencies": [],
            "game_versions": [manifest["minecraft"]],
            "loaders": spec["loaders"],
            "version_type": "beta" if "-" in manifest["version"] else "release",
            "featured": False,
            "file_parts": parts,
            "primary_file": parts[0],
        }
        with ExitStack() as stack:
            files = {
                part: (
                    name,
                    stack.enter_context((assets / name).open("rb")),
                    "application/octet-stream",
                )
                for part, name in zip(parts, spec["files"])
            }
            response = session.post(
                "https://api.modrinth.com/v2/version",
                data={"data": json.dumps(data)},
                files=files,
                timeout=120,
            )
            response.raise_for_status()
            print(f"Published Modrinth version {response.json()['id']}: {spec['number']}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("destination", choices=["github", "modrinth"])
    parser.add_argument("--assets", type=Path, required=True)
    parser.add_argument("--variant", choices=["all", "main", "geyser"], default="all")
    parser.add_argument("--confirm", action="store_true")
    parser.add_argument("--tag", help="Require this tag to match the artifact version")
    args = parser.parse_args()
    try:
        manifest = verify(args.assets)
        if args.tag and args.tag != "v" + manifest["version"]:
            raise ValueError("Requested tag does not match version.txt / release assets")
        if not args.confirm:
            print(
                json.dumps(
                    {
                        "dry_run": True,
                        "destination": args.destination,
                        "manifest": manifest,
                        "modrinth_variants": modrinth_variants(manifest),
                    },
                    indent=2,
                )
            )
            return
        if args.destination == "github":
            github(args.assets, manifest)
        else:
            modrinth(args.assets, manifest, args.variant)
    except (
        ValueError,
        requests.RequestException,
        subprocess.CalledProcessError,
    ) as error:
        parser.error(str(error))


if __name__ == "__main__":
    main()
