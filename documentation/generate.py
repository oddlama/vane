#!/usr/bin/env python3

import argparse
from pathlib import Path

def main():
    parser = argparse.ArgumentParser(description="Generates the documentation page.")
    parser.add_argument('-o', '--output-dir', dest='output_dir', default="build", type=str,
            help="Specifies the output directory for the documentation. (default: 'build')")
    args = parser.parse_args()

    build_path = Path(args.output_dir)
    build_path.mkdir(parents=True, exist_ok=True)

    assets_path = build_path / "assets"
    assets_path.mkdir(parents=True, exist_ok=True)

    with open(build_path / "index.html", "w") as f:
        f.write(open("templates/index.html", "r").read())

if __name__ == "__main__":
    main()
