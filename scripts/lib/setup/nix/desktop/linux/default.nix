{ stdenv, pkgs }:

with pkgs;
with stdenv;

let
  baseImage = callPackage ./base-image { };
  linuxdeployqt = callPackage ./linuxdeployqt { inherit appimagekit; };

in
{
  buildInputs = [ appimagekit linuxdeployqt patchelf baseImage ];
}
