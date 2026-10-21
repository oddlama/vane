{
  pkgs,
  plugins,
  jdk,
  paper,
}:
let
  version = pkgs.lib.trim (builtins.readFile ../version.txt);
  runner = pkgs.writeShellApplication {
    name = "vane-testserver";
    runtimeInputs = [ pkgs.python3 ];
    text = ''
      exec python ${./scripts/testserver.py} --paper ${paper} --java ${jdk}/bin/java --version ${pkgs.lib.escapeShellArg version} "$@"
    '';
  };
  testserver = pkgs.writeShellApplication {
    name = "testserver";
    text = ''
      exec ${runner}/bin/vane-testserver --plugins ${plugins} "$@"
    '';
  };
in
{
  inherit runner testserver;
}
