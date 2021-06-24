#!/bin/bash

specialsource="external/SpecialSource-1.10.0-shaded.jar"
mapping="external/bukkit-e3c5450d-fields.csrg"
paper_jar="libs/patched_1.17.jar"

die() {
    echo "[1;31merror:[m $*" >&2
    exit 1
}

echo "[+] Remapping paper jar"
java -cp "$specialsource" net.md_5.specialsource.SpecialSource --live -i "$paper_jar" -o "${paper_jar/.jar/_remapped.jar}" -m "$mapping" \
    || die "Could not apply mapping"
