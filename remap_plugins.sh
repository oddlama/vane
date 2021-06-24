#!/bin/bash

specialsource="external/SpecialSource-1.10.0-shaded.jar"
mapping="external/bukkit-e3c5450d-fields.csrg"
sign=true

die() {
    echo "[1;31merror:[m $*" >&2
    exit 1
}

echo "[+] Remapping jar files"
for i in target/*.jar; do
    o="${i/target/target-obf}"
    mkdir -p "$(dirname "$o")" \
        || die "Could not create directory"

    java -cp "$specialsource" net.md_5.specialsource.SpecialSource --live -i "$i" -o "$o" -m "$mapping" --reverse \
        || die "Could not apply mapping"
done

if [[ "$sign" == "true" ]]; then
    echo "[+] Signing jar files"
    for i in target-obf/*.jar; do
        gpg --local-user 680AA614E988DE3E84E0DEFA503F6C0684104B0A --armor --detach-sign --sign "$i" \
            || die "Could not sign jar file"
    done
fi

echo "[+] Creating all-plugins.zip"
cd target-obf \
    || die "Could not cd into target-obf/"
zip -r all-plugins.zip vane*.jar -x "vane*waterfall*.jar" \
    || die "Could not create all-plugins.zip"
