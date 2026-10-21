{
  lib,
  stdenvNoCC,
  fetchurl,
  jdk,
}:
let
  paper = fetchurl {
    url = "https://fill-data.papermc.io/v1/objects/2df3b846f70716907d380915cc026667a9cadedf99b1ec680b0c5ff0e6132526/paper-26.3-207.jar";
    hash = "sha256:2df3b846f70716907d380915cc026667a9cadedf99b1ec680b0c5ff0e6132526";
  };
  minecraft = fetchurl {
    url = "https://piston-data.mojang.com/v1/objects/33680f5f2ac32864d6d7cf5e56a705fdb3e05f4c/server.jar";
    hash = "sha256:d052f14d7a173734fba553711e5b570162e2f2a313267ee31a21b975a679be64";
  };
in
stdenvNoCC.mkDerivation {
  pname = "vane-paper-runtime";
  version = "26.3-207";
  dontUnpack = true;
  nativeBuildInputs = [ jdk ];
  buildPhase = ''
    mkdir -p prepared/cache
    cp ${paper} prepared/paper.jar
    cp ${minecraft} prepared/cache/mojang_26.3.jar
    cd prepared
    # Apply/extract Paperclip without launching Minecraft or accepting its EULA.
    java -Dpaperclip.patchonly=true -jar paper.jar
  '';
  installPhase = ''
    mkdir -p "$out"
    cp -r . "$out/"
  '';
  meta = {
    description = "Pinned, prepatched Paper runtime for disposable development servers";
    license = [
      lib.licenses.gpl3Only
      lib.licenses.unfreeRedistributable
    ];
  };
}
