#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2023-2026 IacobIacob01
# SPDX-License-Identifier: Apache-2.0
#
# Cross-compiles x265 (HEVC encoder) + aom (AV1 encoder) + libde265 (HEVC decoder) + libheif
# (WITH encoders) as static libs for Android, installing into
#   app/src/main/cpp/heifenc/<abi>/{include,lib}
# so the app's CMakeLists can link the `heifenc` JNI library for memory-bounded TILED HEIC/AVIF
# encode (libheif grid images: each tile encoded separately, whole decoded image never in RAM).
#
# This is intentionally SEPARATE from build-heif.sh (decode-only, cpp/heif/) so the existing tiled
# DECODE path is never affected by the much heavier encoder build.
#
#   scripts/native/build-heif-encode.sh            # arm64-v8a
#   scripts/native/build-heif-encode.sh all        # all four ABIs
#
# Requires: internet access (curl + git), the pinned NDK, and SDK CMake 3.31.x.
set -euo pipefail

# --- Pinned versions -------------------------------------------------------------------------
LIBDE265_VERSION="1.0.15"
LIBHEIF_VERSION="1.19.8"
X265_VERSION="3.6"
AOM_VERSION="v3.9.1"
ANDROID_API=29
CMAKE_VERSION="3.31.6"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
OUT_ROOT="$REPO_ROOT/app/src/main/cpp/heifenc"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ] && [ -f "$REPO_ROOT/local.properties" ]; then
    SDK="$(grep -E '^sdk\.dir=' "$REPO_ROOT/local.properties" | head -n1 | cut -d'=' -f2-)"
fi
if [ -z "$SDK" ] || [ ! -d "$SDK" ]; then
    echo "ERROR: Android SDK not found. Set ANDROID_HOME or sdk.dir in local.properties." >&2
    exit 1
fi

NDK_DIR=""
for cand in "$SDK/ndk/29.0.14033849" "$SDK/ndk/29."* ; do
    if [ -d "$cand" ]; then NDK_DIR="$cand"; break; fi
done
if [ -z "$NDK_DIR" ]; then
    NDK_DIR="$(ls -d "$SDK/ndk/"* 2>/dev/null | sort -V | tail -n1 || true)"
fi
if [ -z "$NDK_DIR" ] || [ ! -d "$NDK_DIR" ]; then
    echo "ERROR: No NDK found under $SDK/ndk. Install NDK r29." >&2
    exit 1
fi
TOOLCHAIN="$NDK_DIR/build/cmake/android.toolchain.cmake"

CMAKE_BIN="$SDK/cmake/$CMAKE_VERSION/bin/cmake"
NINJA_BIN="$SDK/cmake/$CMAKE_VERSION/bin/ninja"
if [ ! -x "$CMAKE_BIN" ]; then
    CMAKE_BIN="$(command -v cmake || true)"
    NINJA_BIN="$(command -v ninja || true)"
fi
if [ -z "$CMAKE_BIN" ]; then echo "ERROR: cmake $CMAKE_VERSION not found." >&2; exit 1; fi

echo "SDK      = $SDK"
echo "NDK      = $NDK_DIR"
echo "CMake    = $CMAKE_BIN"
echo "Out      = $OUT_ROOT"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
DE265_SRC="$WORK/libde265-$LIBDE265_VERSION"
HEIF_SRC="$WORK/libheif-$LIBHEIF_VERSION"
X265_SRC="$WORK/x265_$X265_VERSION"
AOM_SRC="$WORK/aom"

echo "==> Downloading libde265 $LIBDE265_VERSION"
curl -fsSL "https://github.com/strukturag/libde265/releases/download/v$LIBDE265_VERSION/libde265-$LIBDE265_VERSION.tar.gz" | tar -xz -C "$WORK"
echo "==> Downloading libheif $LIBHEIF_VERSION"
curl -fsSL "https://github.com/strukturag/libheif/releases/download/v$LIBHEIF_VERSION/libheif-$LIBHEIF_VERSION.tar.gz" | tar -xz -C "$WORK"
echo "==> Downloading x265 $X265_VERSION"
curl -fsSL "https://bitbucket.org/multicoreware/x265_git/downloads/x265_$X265_VERSION.tar.gz" | tar -xz -C "$WORK" \
    || curl -fsSL "https://download.videolan.org/pub/videolan/x265/x265_$X265_VERSION.tar.gz" | tar -xz -C "$WORK"
echo "==> Cloning aom $AOM_VERSION"
git clone --depth 1 --branch "$AOM_VERSION" https://aomedia.googlesource.com/aom "$AOM_SRC"

build_abi() {
    local ABI="$1"
    echo "================================================================"
    echo "==> Building HEIC/AVIF encoders for $ABI"
    echo "================================================================"
    local STAGE="$WORK/stage-$ABI"
    local OUT="$OUT_ROOT/$ABI"
    rm -rf "$OUT"; mkdir -p "$OUT/lib" "$OUT/include"

    local COMMON=(
        -G Ninja
        -DCMAKE_MAKE_PROGRAM="$NINJA_BIN"
        -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN"
        -DANDROID_ABI="$ABI" -DANDROID_PLATFORM="android-$ANDROID_API"
        -DCMAKE_BUILD_TYPE=Release
        -DBUILD_SHARED_LIBS=OFF
        -DCMAKE_INSTALL_PREFIX="$STAGE"
        -DCMAKE_PREFIX_PATH="$STAGE"
        -DCMAKE_FIND_ROOT_PATH="$STAGE"
    )

    # 1) libde265 (HEVC decoder — libheif needs a decoder present)
    "$CMAKE_BIN" -S "$DE265_SRC" -B "$WORK/de265-$ABI" "${COMMON[@]}" \
        -DENABLE_SDL=OFF -DENABLE_DECODER=OFF -DENABLE_ENCODER=OFF
    "$CMAKE_BIN" --build "$WORK/de265-$ABI" --target install

    # 2) x265 (HEVC encoder). Assembly OFF for portable, reliable cross-compile.
    "$CMAKE_BIN" -S "$X265_SRC/source" -B "$WORK/x265-$ABI" "${COMMON[@]}" \
        -DENABLE_SHARED=OFF -DENABLE_CLI=OFF -DENABLE_ASSEMBLY=OFF -DHIGH_BIT_DEPTH=OFF
    "$CMAKE_BIN" --build "$WORK/x265-$ABI" --target install
    # x265 installs libx265.a; ensure the unversioned name exists.
    [ -f "$STAGE/lib/libx265.a" ] || { echo "ERROR: libx265.a not built" >&2; exit 1; }

    # 3) aom (AV1 encoder + decoder)
    "$CMAKE_BIN" -S "$AOM_SRC" -B "$WORK/aom-$ABI" "${COMMON[@]}" \
        -DENABLE_TESTS=OFF -DENABLE_EXAMPLES=OFF -DENABLE_DOCS=OFF -DENABLE_TOOLS=OFF \
        -DCONFIG_AV1_ENCODER=1 -DCONFIG_AV1_DECODER=1 -DAOM_TARGET_CPU=arm64
    "$CMAKE_BIN" --build "$WORK/aom-$ABI" --target install

    # 4) libheif WITH encoders, pointed at staged x265 + aom + libde265.
    "$CMAKE_BIN" -S "$HEIF_SRC" -B "$WORK/heif-$ABI" "${COMMON[@]}" \
        -DWITH_EXAMPLES=OFF -DWITH_GDK_PIXBUF=OFF -DBUILD_TESTING=OFF \
        -DWITH_LIBDE265=ON -DENABLE_PLUGIN_LOADING=OFF \
        -DWITH_X265=ON \
        -DWITH_AOM_ENCODER=ON -DWITH_AOM_DECODER=ON -DWITH_DAV1D=OFF -DWITH_RAV1E=OFF -DWITH_SvtEnc=OFF \
        -DWITH_JPEG_DECODER=OFF -DWITH_JPEG_ENCODER=OFF \
        -DWITH_OpenJPEG_DECODER=OFF -DWITH_OpenJPEG_ENCODER=OFF \
        -DWITH_UNCOMPRESSED_CODEC=OFF \
        -DLIBDE265_INCLUDE_DIR="$STAGE/include" -DLIBDE265_LIBRARY="$STAGE/lib/libde265.a" \
        -DX265_INCLUDE_DIR="$STAGE/include" -DX265_LIBRARY="$STAGE/lib/libx265.a" \
        -DAOM_INCLUDE_DIR="$STAGE/include" -DAOM_LIBRARY="$STAGE/lib/libaom.a" \
        -DPKG_CONFIG_EXECUTABLE=/usr/bin/pkg-config
    "$CMAKE_BIN" --build "$WORK/heif-$ABI" --target install

    # 5) Collect static libs + headers.
    cp -f "$STAGE/lib/libheif.a" "$OUT/lib/"
    cp -f "$STAGE/lib/libde265.a" "$OUT/lib/"
    cp -f "$STAGE/lib/libx265.a" "$OUT/lib/"
    cp -f "$STAGE/lib/libaom.a" "$OUT/lib/"
    cp -Rf "$STAGE/include/libheif" "$OUT/include/"
    echo "==> Installed $ABI: $(ls -1 "$OUT/lib")"
}

declare -a ABIS=()
add_abi() { local a="$1"; for e in "${ABIS[@]:-}"; do [ "$e" = "$a" ] && return; done; ABIS+=("$a"); }
if [ "$#" -eq 0 ]; then add_abi "arm64-v8a"; else
    for arg in "$@"; do case "$(echo "$arg" | tr '[:upper:]' '[:lower:]')" in
        all|universal) add_abi "arm64-v8a"; add_abi "armeabi-v7a"; add_abi "x86_64"; add_abi "x86" ;;
        arm64-v8a|arm64) add_abi "arm64-v8a" ;;
        armeabi-v7a|arm) add_abi "armeabi-v7a" ;;
        x86_64) add_abi "x86_64" ;;
        x86) add_abi "x86" ;;
        ci) : ;;
        *) echo "WARNING: unknown ABI arg '$arg', skipping" >&2 ;;
    esac; done
fi
if [ "${#ABIS[@]}" -eq 0 ]; then add_abi "arm64-v8a"; fi

for abi in "${ABIS[@]}"; do build_abi "$abi"; done
echo "DONE. Prebuilt encoder libs under: $OUT_ROOT"
