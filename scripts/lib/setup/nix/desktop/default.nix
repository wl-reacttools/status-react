{ stdenv, pkgs }:

with pkgs;
with stdenv; 

let
  windowsPlatform = callPackage ./windows { };
  appimagekit = callPackage ./appimagetool { };
  linuxdeployqt = callPackage ./linuxdeployqt { appimagekit = appimagekit; };

in
  {
    buildInputs = [
      cmake
      extra-cmake-modules
      go
      qt5.full
    ] ++ lib.optional isLinux [ appimagekit linuxdeployqt patchelf ]
      ++ lib.optional isLinux windowsPlatform.buildInputs;
    shellHook = ''
      export QT_PATH="${qt5.full}"
      export PATH="${qt5.full}/bin:$PATH"
    '';
  }
