{
  lib,
  python3,
  fetchPypi,
}:
let
  ps = python3.pkgs;
  busylib = ps.buildPythonPackage {
    pname = "busylib";
    version = "2.6.0";
    pyproject = true;
    src = fetchPypi {
      pname = "busylib";
      version = "2.6.0";
      hash = "sha256:961780dc782aa4db26f0c19dc80751b1389912838fc56b26d00855cf40013493";
    };
    build-system = [
      ps.setuptools
      ps.setuptools-scm
    ];
    dependencies = with ps; [
      httpx2
      protobuf
      pydantic
      pydantic-extra-types
      pydantic-settings
      typing-extensions
      websockets
      zeroconf
    ];
    doCheck = false; # Upstream integration tests require a physical BUSY Bar.
    pythonImportsCheck = [
      "busylib"
      "busylib.features"
    ];
    meta.license = lib.licenses.mit;
  };
  version = lib.trim (builtins.readFile ../version.txt);
  bridge = ps.buildPythonPackage {
    pname = "barmc";
    inherit version;
    pyproject = true;
    src = lib.cleanSource ../vane-busybar/bridge;
    build-system = [ ps.setuptools ];
    dependencies = [
      busylib
      ps.httpx
      ps.pillow
    ];
    postPatch = ''
      substituteInPlace pyproject.toml --replace-fail 'version = "${(lib.importTOML ../vane-busybar/bridge/pyproject.toml).project.version}"' 'version = "${version}"'
    '';
    doCheck = false; # Each maintained test file is a separate flake check.
    pythonImportsCheck = [
      "barmc"
      "barmc.display.busybar"
      "barmc.pairing"
    ];
    meta = {
      mainProgram = "barmc";
      license = lib.licenses.mit;
    };
  };
  testPython = python3.withPackages (_: [
    bridge
    ps.pytest
  ]);
in
{
  inherit bridge busylib testPython;
}
