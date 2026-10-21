#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
exec nix build .#website "$@"
