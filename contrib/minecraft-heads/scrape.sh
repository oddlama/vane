#!/bin/bash

set -eo pipefail

die() {
	echo "[1;31merror:[m $*" >&2
	exit 1
}

DATA_DIR="data"
mkdir -p "$DATA_DIR" || die "Could not create data directory"

download() {
	local num="$1"
	html="$(curl -s -o - "https://minecraft-heads.com/custom-heads/$num")" \
		|| return 1

	texture="$(grep -o -m1 'eyJ0ZXh0dXJlcyI6eyJTS0lOIj[A-Za-z0-9+/=]*' <<< "$html")" \
		|| return 1

	categories="[$(grep -oP 'href="/custom-heads/tags/var/[^<]*>\K.*(?=</a>,)' <<< "$html" \
		| xargs -d'\n' -L1 printf '"%s"\n' | paste -sd "," -)]" \
		|| return 1

	name="$(grep -A 1 '<!-- IMAGE START -->' <<< "$html" | tail -1)" \
		|| return 1
	name=${name%%'" style'*}
	name=${name##*' alt="'}

	category="$(grep -A 2 '<!-- CATEGORY START -->' <<< "$html" | tail -1)" \
		|| return 1
	category=${category%%'">'*}
	category=${category##*'/custom-heads/'}

	cat > "$DATA_DIR/$num" <<EOF
{
"name": "$name",
"texture": "$texture",
"category": "$category",
"categories": $categories
}
EOF

	return 0
}

id=0
max=40000
while [[ $id -lt $max ]]; do
	printf "\r$id/$max"
	if ! download "$id"; then
		echo "$id" >> errors
		echo
		echo "error in $id"
	fi
	((++id))
done
