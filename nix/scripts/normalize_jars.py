"""Normalize ZIP metadata in exported test dependencies; preserve every entry's bytes.

Paperweight generates mappedServerJar.jar with wall-clock timestamps and varying
entry order. These affect the test-runtime output, not the distributable plugins.
"""

import hashlib
from pathlib import Path
import shutil
import sys
from tempfile import TemporaryDirectory
import zipfile


def normalize_archive(source, destination):
    with zipfile.ZipFile(source) as original, zipfile.ZipFile(destination, "w") as normalized:
        if len(set(original.namelist())) != len(original.namelist()):
            raise ValueError(f"Duplicate archive entries in {source}")

        # JarInputStream expects the manifest at the beginning, unlike plain ZIP readers.
        def order(entry):
            return (
                0 if entry.filename == "META-INF/" else 1 if entry.filename == "META-INF/MANIFEST.MF" else 2,
                entry.filename,
            )

        for entry in sorted(original.infolist(), key=order):
            info = zipfile.ZipInfo(entry.filename, (1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = (0o40755 if entry.is_dir() else 0o100644) << 16
            normalized.writestr(info, original.read(entry))


def normalize_tree(root):
    with TemporaryDirectory() as directory:
        cache = {}
        for jar in sorted(root.rglob("*.jar")):
            digest = hashlib.sha256(jar.read_bytes()).hexdigest()
            if digest not in cache:
                cache[digest] = Path(directory) / (digest + ".jar")
                normalize_archive(jar, cache[digest])
            shutil.copyfile(cache[digest], jar)


if __name__ == "__main__":
    normalize_tree(Path(sys.argv[1]))
