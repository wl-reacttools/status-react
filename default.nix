let
  pkgs = import ((import <nixpkgs> { }).fetchFromGitHub {
    owner = "status-im";
    repo = "nixpkgs";
    rev = "5922f168965214ffdbc1404c081dca95b714840b";
    sha256 = "1jrvbf0903bs6a74x1nwxa6bx0q78a420mkw5104p2p5967lxkvg";
  }) { config = { }; };

in with pkgs;
  let
    derivation = if stdenv.isLinux then stdenvNoCC.mkDerivation else stdenv.mkDerivation;
    statusDesktop = callPackage ./scripts/lib/setup/nix/desktop { };
    statusMobile = callPackage ./scripts/lib/setup/nix/mobile { };
    nodeInputs = import ./scripts/lib/setup/nix/global-node-packages/output {
      # The remaining dependencies come from Nixpkgs
      inherit pkgs;
      inherit nodejs;
    };
    nodePkgs = [
      nodejs
      python27 # for e.g. gyp
      yarn
    ] ++ (map (x: nodeInputs."${x}") (builtins.attrNames nodeInputs));

  in derivation rec {
    name = "env";
    env = buildEnv { name = name; paths = buildInputs; };
    buildInputs = with stdenv; [
      bash
      clojure
      curl
      git
      jq
      leiningen
      maven
      statusDesktop.buildInputs
      statusMobile.buildInputs
      watchman
      unzip
      wget
    ] ++ nodePkgs
      ++ lib.optional isDarwin cocoapods
      ++ lib.optional isLinux gcc7;
    shellHook = ''
        ${statusDesktop.shellHook}
        ${statusMobile.shellHook}

        [ -d "$ANDROID_SDK_ROOT" ] || ./scripts/setup # we assume that if the Android SDK dir does not exist, make setup needs to be run
    '';
  }
