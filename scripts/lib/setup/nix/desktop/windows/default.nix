{ stdenv, pkgs }:

with pkgs;
with stdenv; 

let
  conan = callPackage ./conan { };
  nsis = callPackage ./nsis { };

in
  {
    buildInputs = lib.optional isLinux [ conan nsis ];
  }
