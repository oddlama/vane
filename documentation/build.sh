#!/usr/bin/env bash

function die() { echo "[1;31merror:[m $*" >&2; exit 1; }

[[ -e minecraft-client.jar ]] \
	|| die "Please copy a recent minecraft client jar file to ./minecraft-client.jar. It is required for item assets."

echo "Generating content..."
./generate.py --client-jar minecraft-client.jar --plugins-dir plugins -o ../docs

echo "Generating css..."
npx tailwindcss -i templates/style.css -o ../docs/css/style.css --minify
