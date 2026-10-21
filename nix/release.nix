{
  lib,
  runCommand,
  python3,
  writeText,
  plugins,
  bridge,
  revision,
}:
let
  catalog = lib.importTOML ../gradle/libs.versions.toml;
  metadata = writeText "release-metadata.json" (
    builtins.toJSON {
      version = lib.trim (builtins.readFile ../version.txt);
      inherit revision;
      minecraft = builtins.head (lib.splitString ".build." catalog.versions.paper);
      repository = "oddlama/vane";
      modrinth_project = "698NGGtb";
    }
  );
in
runCommand "vane-release" { nativeBuildInputs = [ python3 ]; } ''
  python ${./scripts/release.py} ${plugins} ${bridge.dist} "$out" ${metadata}
''
