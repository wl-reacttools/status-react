{ stdenv, pkgs }:

with pkgs;
with stdenv; 

let
  windowsPlatform = callPackage ./windows { };
  appimagetool = callPackage ./appimagetool { };
  linuxdeployqt = callPackage ./linuxdeployqt { };

in
  {
    buildInputs = [
      cmake
      extra-cmake-modules
      go
      qt5.full
    ] ++ lib.optional isLinux [ appimagetool linuxdeployqt patchelf ]
      ++ windowsPlatform.buildInputs;
    shellHook = ''
      export QT_PATH="${qt5.full}"
    '';
  }
