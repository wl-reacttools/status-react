{ pkgs ? import <nixpkgs> { },
  dpkg ? pkgs.dpkg,
  stdenv ? pkgs.stdenv,
  fetchurl ? pkgs.fetchurl }:

with pkgs;

stdenv.mkDerivation rec {
  name = "nsis-${version}";
  version = "2.50-1";

  cclib = stdenv.cc.cc.lib;
  myPackages = buildEnv {
    name = name;
    paths = [ cclib zlib ];
  };

  src =
    if stdenv.hostPlatform.system == "x86_64-linux" then
      fetchurl {
        url = "http://archive.ubuntu.com/ubuntu/pool/universe/n/nsis/nsis_${version}_amd64.deb";
        sha256 = "1zi3s06jzc2ghk3kb1cnvgkhb0yzqwrlgn3f4zqqzkr4xxdpi3pp";
      }
    else throw "${name} is not supported on ${stdenv.hostPlatform.system}";
  srcCommon =
    if stdenv.hostPlatform.system == "x86_64-linux" then
      fetchurl {
        url = "http://archive.ubuntu.com/ubuntu/pool/universe/n/nsis/nsis-common_${version}_all.deb";
        sha256 = "0ari301szb6swjvrn9m7ar3yy945hizbld6h6yxjdmj3ndczkgz6";
      }
    else throw "${name} is not supported on ${stdenv.hostPlatform.system}";

  nativeBuildInputs = [ dpkg ];
  buildInputs = [ makeWrapper ];

  unpackPhase = "dpkg-deb -x ${src} ./ && dpkg-deb -x ${srcCommon} ./";

  dontStrip = true;
  installPhase = ''
    mkdir $out
    mv usr/* $out/
    mv etc $out
    rmdir usr
    mv * $out/

    # patch and link binaries
    patchelf --interpreter "$(cat $NIX_CC/nix-support/dynamic-linker)" \
             --set-rpath "${zlib}/lib:${cclib}/lib" \
             $out/bin/makensis
    patchelf --interpreter "$(cat $NIX_CC/nix-support/dynamic-linker)" \
             --set-rpath "${zlib}/lib:${cclib}/lib" \
             $out/bin/GenPat
    patchelf --interpreter "$(cat $NIX_CC/nix-support/dynamic-linker)" \
             --set-rpath "${cclib}/lib" \
             $out/bin/LibraryLocal

    mv $out/bin/makensis $out/bin/makensis-wrapped
    makeWrapper "$out/bin/makensis-wrapped" "$out/bin/makensis" \
      --set NSISDIR "$out/share/nsis" \
      --set NSISCONFDIR "$out/etc"
  '';

  meta = {
    description = "NSIS is a free scriptable win32 installer/uninstaller system that doesn't suck and isn't huge";
    homepage = https://nsis.sourceforge.io/;
    license = stdenv.lib.licenses.zlib;
    maintainers = [ stdenv.lib.maintainers.pombeirp ];
    platforms = stdenv.lib.platforms.linux;
  };
}
