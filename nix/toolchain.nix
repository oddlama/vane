{ pkgs }:
let
  jdk = pkgs.jdk25;
  gradle =
    (pkgs.gradle-packages.mkGradle {
      version = "9.8.2";
      hash = "sha256:21bd5da2de49b642904077f38c0efdc7778e92b3ed0889392dca61b97e389607";
      defaultJava = jdk;
    }).wrapped;
in
{
  inherit jdk gradle;
}
