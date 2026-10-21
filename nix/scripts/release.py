"""Assemble deterministic, platform-neutral release assets from Nix outputs."""

import hashlib
import json
from pathlib import Path
import shutil
import sys
import zipfile

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


def assemble(plugins, bridge_dist, output, metadata):
    output.mkdir(parents=True, exist_ok=True)
    version = metadata["version"]
    paper_jars = [f"vane-{module}-{version}.jar" for module in PAPER_MODULES]
    jars = paper_jars + [
        f"vane-velocity-{version}.jar",
        f"vane-geyser-extension-{version}.jar",
    ]
    for name in jars + [f"barmc-{version}.zip"]:
        shutil.copyfile(plugins / name, output / name)
    wheels = sorted(bridge_dist.glob("*.whl"))
    if len(wheels) != 1:
        raise ValueError("Expected exactly one barmc wheel")
    shutil.copyfile(wheels[0], output / wheels[0].name)
    with zipfile.ZipFile(output / "all-plugins.zip", "w") as archive:
        for name in sorted(paper_jars):
            info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, (output / name).read_bytes())
    hashes = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(output.iterdir()) if p.is_file()}
    (output / "SHA256SUMS").write_text("".join(f"{digest}  {name}\n" for name, digest in hashes.items()))
    (output / "manifest.json").write_text(json.dumps(metadata | {"artifacts": hashes}, indent=2, sort_keys=True) + "\n")


if __name__ == "__main__":
    assemble(
        Path(sys.argv[1]),
        Path(sys.argv[2]),
        Path(sys.argv[3]),
        json.loads(Path(sys.argv[4]).read_text()),
    )
