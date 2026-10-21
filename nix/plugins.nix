{
  lib,
  stdenv,
  gradle,
  jdk,
  unzip,
  zip,
  python3,
}:
stdenv.mkDerivation (finalAttrs: {
  pname = "vane";
  version = lib.trim (builtins.readFile ../version.txt);
  outputs = [
    "out"
    "tests"
  ];
  src = lib.cleanSourceWith {
    src = ../.;
    name = "vane-source";
    filter =
      path: type:
      let
        name = baseNameOf path;
        relative = lib.removePrefix "${toString ../.}/" path;
        top = builtins.head (lib.splitString "/" relative);
        sourceInput =
          relative == "contrib"
          || lib.hasPrefix "contrib/assets/" relative
          || relative == "contrib/assets"
          || builtins.elem top [
            "build.gradle.kts"
            "settings.gradle.kts"
            "gradle.properties"
            "gradle"
            "version.txt"
          ]
          || lib.hasPrefix "vane-" top
          || relative == "docs"
          || relative == "docs/resourcepacks"
          || relative == "docs/resourcepacks/v${finalAttrs.version}.zip";
      in
      sourceInput
      && !(
        builtins.elem name [
          ".git"
          ".jj"
          ".pi"
          ".gradle"
          ".kotlin"
          ".direnv"
          ".venv"
          "__pycache__"
          ".pytest_cache"
          "node_modules"
          "build"
          "target"
          "run"
          "result"
        ]
        || lib.hasPrefix "result-" name
      );
  };
  nativeBuildInputs = [
    gradle
    unzip
    zip
    python3
  ];
  strictDeps = true;
  mitmCache = gradle.fetchDeps {
    pkg = finalAttrs.finalPackage;
    data = ./gradle-deps.json;
  };
  __darwinAllowLocalNetworking = true;
  env.JAVA_HOME = "${jdk}";
  gradleFlags = [
    "--no-configuration-cache"
    "--max-workers=2"
    "-Dorg.gradle.java.installations.auto-download=false"
    "-Dorg.gradle.java.installations.paths=${jdk}"
    "-Pkotlin.compiler.execution.strategy=in-process"
  ];
  gradleBuildTask = "dist nixTestRuntimes";
  gradleUpdateTask = "dist nixTestRuntimes";
  installPhase = ''
    runHook preInstall
    mkdir -p "$out"
    cp target/* "$out/"
    mkdir -p "$tests"
    for runtime in vane-*/build/nix-test-runtime; do
      module="''${runtime%%/*}"
      cp -r "$runtime" "$tests/$module"
    done
    python ${./scripts/normalize_jars.py} "$tests"
    runHook postInstall
  '';
  meta = {
    description = "Vane Minecraft plugin suite";
    license = lib.licenses.mit;
    platforms = lib.platforms.unix;
  };
})
