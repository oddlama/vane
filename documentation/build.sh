#!/bin/bash

echo "Generating content..."
./generate.py -o ../docs

echo "Generating css..."
npx tailwindcss -i templates/style.css -o ../docs/css/style.css --minify
