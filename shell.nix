{ pkgs ? import <nixpkgs> {},
  target-os ? "all" }:
with pkgs;

let
  projectDeps = import ./default.nix { inherit target-os; };
  platform = callPackage ./nix/platform.nix { inherit target-os; };
  useFastlanePkg = platform.targetMobile;
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
    file
    git
    gnumake
    jq
    ncurses
    lsof # used in scripts/start-react-native.sh
    ps # used in scripts/start-react-native.sh
    unzip
    wget
  ] ++ lib.optionals useFastlanePkg [ bundler _fastlane.package ]
  ++ lib.optional (_stdenv.isDarwin && useFastlanePkg) clang;
  inputsFrom = [ projectDeps ];
  TARGET_OS=target-os;
  shellHook =
    ''
      set -e

      STATUS_REACT_HOME=$(git rev-parse --show-toplevel)

      ${projectDeps.shellHook}
      ${lib.optionalString useFastlanePkg _fastlane.shellHook}

      if [ "$IN_NIX_SHELL" != 'pure' ] && [ ! -f $STATUS_REACT_HOME/.ran-setup ]; then
        $STATUS_REACT_HOME/scripts/setup
        touch $STATUS_REACT_HOME/.ran-setup
      fi
      set +e
    '';
}