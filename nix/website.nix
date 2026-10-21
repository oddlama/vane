{
  lib,
  stdenvNoCC,
  python3,
  tailwindcss,
}:
stdenvNoCC.mkDerivation {
  pname = "vane-website";
  version = lib.trim (builtins.readFile ../version.txt);
  src = lib.cleanSource ../docs;
  nativeBuildInputs = [
    (python3.withPackages (ps: [ ps.markdown ]))
    tailwindcss
  ];
  strictDeps = true;
  buildPhase = ''
    runHook preBuild
    python generate.py -o build --resource-packs resourcepacks
    tailwindcss --config tailwind.config.js --input templates/style.css --output build/css/style.css --minify
    python -c 'from pathlib import Path; from generate import validate_site; validate_site(Path("build"))'
    runHook postBuild
  '';
  installPhase = ''
    runHook preInstall
    cp -r build "$out"
    runHook postInstall
  '';
  meta.license = lib.licenses.mit;
}
