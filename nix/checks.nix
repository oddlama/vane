{
  lib,
  pkgs,
  plugins,
  jdk,
  bridgePython,
  website,
}:
let
  modules = builtins.filter (
    name: lib.hasPrefix "vane-" name && builtins.pathExists (../. + "/${name}/src/test/kotlin")
  ) (builtins.attrNames (builtins.readDir ../.));
  suites = lib.concatMap (
    module:
    let
      root = ../. + "/${module}/src/test/kotlin";
    in
    map
      (file: {
        inherit module;
        class = lib.replaceStrings [ "/" ] [ "." ] (
          lib.removeSuffix ".kt" (lib.removePrefix "${toString root}/" (toString file))
        );
        name = "jvm-${lib.removePrefix "vane-" module}-${lib.removeSuffix ".kt" (baseNameOf file)}";
      })
      (builtins.filter (p: lib.hasSuffix "Test.kt" (toString p)) (lib.filesystem.listFilesRecursive root))
  ) modules;
  jvm = builtins.listToAttrs (
    map (suite: {
      inherit (suite) name;
      value =
        pkgs.runCommand suite.name
          {
            nativeBuildInputs = [ jdk ];
            __darwinAllowLocalNetworking = true;
          }
          ''
            set -euo pipefail
            runtime=${plugins.tests}/${suite.module}
            grep -Fx ${lib.escapeShellArg suite.class} "$runtime/classes"
            classpath=""
            while IFS= read -r entry; do classpath="$classpath$runtime/$entry:"; done < "$runtime/classpath"
            mkdir -p "$out"
            java -Xmx512m -Djava.io.tmpdir="$TMPDIR" -cp "$classpath" org.junit.runner.JUnitCore ${lib.escapeShellArg suite.class} | tee "$out/result.txt"
          '';
    }) suites
  );
  bridgeFiles = builtins.filter (
    p: lib.hasPrefix "test_" (baseNameOf p) && lib.hasSuffix ".py" (toString p)
  ) (lib.filesystem.listFilesRecursive ../vane-busybar/bridge/tests);
  bridge = builtins.listToAttrs (
    map (file: {
      name = "bridge-${lib.removeSuffix ".py" (lib.removePrefix "test_" (baseNameOf file))}";
      value =
        pkgs.runCommand "barmc-${baseNameOf file}"
          {
            nativeBuildInputs = [ bridgePython ];
            __darwinAllowLocalNetworking = true;
            SSL_CERT_FILE = "${pkgs.cacert}/etc/ssl/certs/ca-bundle.crt";
          }
          ''
            export HOME="$TMPDIR"
            mkdir -p "$out"
            python -m pytest -p no:cacheprovider ${file} --junitxml="$out/junit.xml"
          '';
    }) bridgeFiles
  );
in
jvm
// bridge
// {
  inherit website;
  build-tools =
    pkgs.runCommand "vane-build-tools-tests"
      {
        nativeBuildInputs = [
          (pkgs.python3.withPackages (ps: [
            ps.pytest
            ps.requests
          ]))
        ];
      }
      ''
        cp -r ${./scripts} scripts
        cp -r ${./tests} tests
        python -m pytest -p no:cacheprovider tests --junitxml="$out/junit.xml"
      '';
  test-inventory = pkgs.runCommand "vane-test-inventory" { } (
    lib.concatMapStringsSep "\n" (module: ''
      diff -u ${
        pkgs.writeText "${module}-expected-tests" (
          lib.concatMapStrings (s: s.class + "\n") (builtins.filter (s: s.module == module) suites)
        )
      } ${plugins.tests}/${module}/classes
    '') modules
    + ''
      mkdir -p "$out"
    ''
  );
  documentation =
    pkgs.runCommand "vane-documentation-tests"
      {
        nativeBuildInputs = [
          (pkgs.python3.withPackages (ps: [
            ps.markdown
            ps.pytest
          ]))
        ];
      }
      ''
        cp -r ${lib.cleanSource ../docs} docs
        chmod -R u+w docs
        cd docs
        python -m pytest -p no:cacheprovider tests --junitxml="$out/junit.xml"
      '';
}
