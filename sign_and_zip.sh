#!/usr/bin/env nix-shell
#!nix-shell -i bash -p bash zip

die() {
    echo "[1;31merror:[m $*" >&2
    exit 1
}

echo "[+] Signing jar files"
for i in target/*.jar; do
	echo "$i" | gpg --local-user 680AA614E988DE3E84E0DEFA503F6C0684104B0A --armor --detach-sign --sign > "$i.asc" \
		|| die "Could not sign jar file"
done

echo "[+] Creating all-plugins.zip"
cd target \
    || die "Could not cd into target/"
rm all-plugins.zip &>/dev/null
zip -r all-plugins.zip vane*.jar -x "vane*plexmap*.jar" -x "vane*velocity*.jar" \
    || die "Could not create all-plugins.zip"
