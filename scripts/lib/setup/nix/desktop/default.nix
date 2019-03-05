{ stdenv, pkgs, target-os }:

with pkgs;
with stdenv; 

let
  targetLinux = {
    "linux" = true;
    "" = true;
  }.${target-os} or false;
  targetWindows = {
    "windows" = true;
    "" = true;
  }.${target-os} or false;
  windowsPlatform = callPackage ./windows { };
  appimagetool = callPackage ./appimagetool { };
  linuxdeployqt = callPackage ./linuxdeployqt { };

in
  {
    buildInputs = [
      cmake
      extra-cmake-modules
      go
    ] ++ lib.optional targetLinux [ appimagetool linuxdeployqt patchelf ]
      ++ lib.optional (! targetWindows) qt5.full
      ++ lib.optional targetWindows windowsPlatform.buildInputs;
    shellHook = if target-os == "windows" then "unset QT_PATH" else ''
      export QT_PATH="${qt5.full}"
    '' + (if isDarwin then ''
      export MACOSX_DEPLOYMENT_TARGET=10.9
    '' else "");
  }
