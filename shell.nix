{ pkgs ? import <nixpkgs> {},
  target-os ? "" }:
with pkgs;

let
  projectDeps = import ./default.nix { inherit target-os; };
  targetMobile = {
    "android" = true;
    "ios" = true;
    "" = true;
  }.${target-os} or false;
  # TODO: Try to use stdenv for iOS. The problem is with building iOS as the build is trying to pass parameters to Apple's ld that are meant for GNU's ld (e.g. -dynamiclib)
  _stdenv = stdenvNoCC;
  _mkShell = mkShell.override { stdenv = _stdenv; };
  _fastlane = callPackage ./fastlane {
    bundlerEnv = _: pkgs.bundlerEnv { 
      name = "fastlane-gems";
      gemdir = ./fastlane;
    };
  };

in _mkShell {
  buildInputs = [
    # utilities
    bash
    curl
    git
    jq
    ncurses
    lsof # used in scripts/start-react-native.sh
    ps # used in scripts/start-react-native.sh
    unzip
    wget
  ] ++ lib.optional targetMobile _fastlane;
  inputsFrom = [ projectDeps ];
  shellHook =
    ''
      set -e
    '' +
    projectDeps.shellHook +
    ''
      export FASTLANE_PLUGINFILE_PATH=$PWD/fastlane/Pluginfile
      export FASTLANE_SCRIPT="${_fastlane}/bin/fastlane" # the ruby package also exposes the fastlane Gem, so we want to make sure we don't rely on PATH ordering to get the right package

      if [ -n "$ANDROID_SDK_ROOT" ] && [ ! -d "$ANDROID_SDK_ROOT" ]; then
        ./scripts/setup # we assume that if the Android SDK dir does not exist, make setup needs to be run
      fi
      set +e
    '';
}