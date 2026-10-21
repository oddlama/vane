"""Stage a loopback-only development server without touching unrelated server data."""

import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

PAPER_MODULES = (
    "admin",
    "bedtime",
    "busybar",
    "core",
    "enchantments",
    "permissions",
    "portals",
    "regions",
    "trifles",
)
MARKER = ".vane-testserver"


def prepare(state, plugins, paper, version, port):
    state = state.resolve()
    if state.exists() and not (state / MARKER).is_file() and any(state.iterdir()):
        raise ValueError(f"Refusing nonempty, unmanaged directory: {state}")
    jars = [plugins / f"vane-{name}-{version}.jar" for name in PAPER_MODULES]
    for jar in jars:
        if not jar.is_file():
            raise ValueError(f"Missing plugin: {jar}")
    runtime = state / ".paper"
    runtime_source = state / ".paper-source"
    if runtime.exists() and (not runtime_source.is_file() or runtime_source.read_text() != str(paper.resolve())):
        raise ValueError("Paper runtime changed or is unmanaged; use a fresh --state-dir (keep old worlds backed up)")
    state.mkdir(parents=True, exist_ok=True)
    (state / MARKER).write_text("Managed disposable vane development server\n")
    destination = state / "plugins"
    destination.mkdir(exist_ok=True)
    if destination.is_symlink():
        raise ValueError("The development plugins directory must not be a symlink")
    manifest = state / ".vane-plugins.json"
    previous = json.loads(manifest.read_text()) if manifest.exists() else []
    for name in previous:
        if Path(name).name != name or not name.startswith("vane-") or not name.endswith(".jar"):
            raise ValueError("Invalid managed plugin manifest")
    for name in previous:
        (destination / name).unlink(missing_ok=True)
    for jar in jars:
        target = destination / jar.name
        if target.is_symlink():
            raise ValueError(f"Refusing plugin symlink: {target}")
        shutil.copyfile(jar, target)
    manifest.write_text(json.dumps([p.name for p in jars]) + "\n")
    # Paperclip's vanilla download and patched libraries are already in the Nix output.
    if runtime.is_symlink():
        raise ValueError("The development runtime directory must not be a symlink")
    if not runtime.exists():
        shutil.copytree(paper, runtime)
        runtime_source.write_text(str(paper.resolve()))
    # Paperclip expects its cache and extracted jars relative to the server cwd.
    for name in ("cache", "libraries", "versions"):
        source = runtime / name
        target = state / name
        if target.is_symlink():
            if target.resolve() != source.resolve():
                raise ValueError(f"Refusing unmanaged runtime link: {target}")
        elif target.exists():
            raise ValueError(f"Refusing unmanaged runtime directory: {target}")
        elif source.exists():
            target.symlink_to(source, target_is_directory=True)
    properties = state / "server.properties"
    if not properties.exists():
        properties.write_text(
            f"server-ip=127.0.0.1\nserver-port={port}\nonline-mode=true\nmotd=Vane development server\n"
        )
    (state / "eula.txt").write_text("eula=true\n")
    return state


def main():
    parser = argparse.ArgumentParser(
        prog="vane-testserver",
        description=__doc__,
        epilog="Starting requires VANE_ACCEPT_EULA=true after accepting https://aka.ms/MinecraftEULA",
    )
    parser.add_argument("--plugins", type=Path, required=True)
    parser.add_argument("--paper", type=Path, required=True)
    parser.add_argument("--java", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--state-dir", type=Path, default=Path(".vane-testserver"))
    parser.add_argument("--port", type=int, default=25565)
    parser.add_argument(
        "--build",
        action="store_true",
        help="Run incremental gradle dist in the current checkout first",
    )
    args = parser.parse_args()
    if os.environ.get("VANE_ACCEPT_EULA") != "true":
        parser.error("Set VANE_ACCEPT_EULA=true only after accepting https://aka.ms/MinecraftEULA")
    if not 1 <= args.port <= 65535:
        parser.error("--port must be between 1 and 65535")
    if args.build:
        subprocess.run(["gradle", "dist"], check=True)
    try:
        state = prepare(args.state_dir, args.plugins, args.paper, args.version, args.port)
    except ValueError as error:
        parser.error(str(error))
    os.chdir(state)
    os.execv(
        args.java,
        [
            args.java,
            "-Xms1G",
            "-Xmx2G",
            "-jar",
            str(state / ".paper/paper.jar"),
            "--nogui",
        ],
    )


if __name__ == "__main__":
    main()
