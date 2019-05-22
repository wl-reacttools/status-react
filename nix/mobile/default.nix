{ config, stdenv, pkgs, callPackage, target-os,
  gradle, status-go, composeXcodeWrapper, nodejs }:

with stdenv;

let
  platform = callPackage ../platform.nix { inherit target-os; };
  xcodewrapperArgs = {
    version = "10.1";
  };
  xcodeWrapper = composeXcodeWrapper xcodewrapperArgs;
  androidPlatform = callPackage ./android.nix { inherit config gradle; };
  selectedSources =
    [ status-go ] ++
    lib.optional platform.targetAndroid androidPlatform;
  nodeInputs = import ./realm-node {
    # The remaining dependencies come from Nixpkgs
    inherit pkgs nodejs;
  };

in
  {
    inherit (androidPlatform) androidComposition;
    inherit xcodewrapperArgs;

    buildInputs =
      status-go.buildInputs ++
      (builtins.attrValues nodeInputs) ++
      lib.catAttrs "buildInputs" selectedSources ++
      lib.optional (platform.targetIOS && isDarwin) xcodeWrapper;
    shellHook = lib.concatStrings (lib.catAttrs "shellHook" selectedSources);
  }
