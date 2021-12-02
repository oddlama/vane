#!/bin/bash

specialsource="external/SpecialSource-1.11.0-shaded.jar"
mapping="external/bukkit-8e9479b6-members.csrg"
paper_jar="libs/patched_1.18.jar"

die() {
    echo "[1;31merror:[m $*" >&2
    exit 1
}

echo "[+] Remapping paper jar"
java -cp "$specialsource" net.md_5.specialsource.SpecialSource --live -i "$paper_jar" -o "${paper_jar/.jar/_remapped.jar}" -m "$mapping" \
    || die "Could not apply mapping"
