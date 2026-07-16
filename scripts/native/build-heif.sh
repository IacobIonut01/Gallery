#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2023-2026 IacobIacob01
# SPDX-License-Identifier: Apache-2.0
#
# Cross-compiles libde265 (HEVC decoder) + libheif (decode-only, static) for Android using the
# NDK CMake toolchain, and installs the resulting static libs + headers into
#   app/src/main/cpp/heif/<abi>/{include,lib}
# so the app's CMakeLists can link them without any build-time downloads.
#
# Run once per machine (and again only when bumping LIBHEIF_VERSION / LIBDE265_VERSION):
#   scripts/native/build-heif.sh            # builds arm64-v8a (device/dev ABI)
#   scripts/native/build-heif.sh all        # builds all four ABIs for release
#
# Requires: internet access, the pinned NDK, and SDK CMake 3.31.x (NOT 4.x — CMake 4 breaks
# libde265's cmake_minimum_required(<3.5)).
set -euo pipefail

# --- Pinned versions -------------------------------------------------------------------------
LIBDE265_VERSION="1.0.15"
LIBHEIF_VERSION="1.19.8"
ANDROID_API=29                          # matches app minSdk
CMAKE_VERSION="3.31.6"                   # must be a 3.x SDK CMake, not 4.x

# --- Paths -----------------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
OUT_ROOT="$REPO_ROOT/app/src/main/cpp/heif"

# Resolve the Android SDK from local.properties (sdk.dir) or the environment.
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ] && [ -f "$REPO_ROOT/local.properties" ]; then
    SDK="$(grep -E '^sdk\.dir=' "$REPO_ROOT/local.properties" | head -n1 | cut -d'=' -f2-)"
fi
if [ -z "$SDK" ] || [ ! -d "$SDK" ]; then
    echo "ERROR: Android SDK not found. Set ANDROID_HOME or sdk.dir in local.properties." >&2
    exit 1
fi

# Pick the NDK: prefer the pinned r29 line, else the highest installed.
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

# --- Source download (cached in a temp dir) --------------------------------------------------
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
DE265_SRC="$WORK/libde265-$LIBDE265_VERSION"
HEIF_SRC="$WORK/libheif-$LIBHEIF_VERSION"

echo "==> Downloading libde265 $LIBDE265_VERSION"
curl -fsSL "https://github.com/strukturag/libde265/releases/download/v$LIBDE265_VERSION/libde265-$LIBDE265_VERSION.tar.gz" \
    | tar -xz -C "$WORK"
echo "==> Downloading libheif $LIBHEIF_VERSION"
curl -fsSL "https://github.com/strukturag/libheif/releases/download/v$LIBHEIF_VERSION/libheif-$LIBHEIF_VERSION.tar.gz" \
    | tar -xz -C "$WORK"

build_abi() {
    local ABI="$1"
    echo "================================================================"
    echo "==> Building for $ABI"
    echo "================================================================"
    local STAGE="$WORK/stage-$ABI"
    local OUT="$OUT_ROOT/$ABI"
    rm -rf "$OUT"
    mkdir -p "$OUT"

    # 1) libde265 (HEVC decoder) -> install to STAGE
    "$CMAKE_BIN" -S "$DE265_SRC" -B "$WORK/de265-$ABI" -G Ninja \
        -DCMAKE_MAKE_PROGRAM="$NINJA_BIN" \
        -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN" \
        -DANDROID_ABI="$ABI" -DANDROID_PLATFORM="android-$ANDROID_API" \
        -DCMAKE_BUILD_TYPE=Release \
        -DBUILD_SHARED_LIBS=OFF \
        -DENABLE_SDL=OFF -DENABLE_DECODER=OFF -DENABLE_ENCODER=OFF \
        -DCMAKE_INSTALL_PREFIX="$STAGE"
    "$CMAKE_BIN" --build "$WORK/de265-$ABI" --target install

    # 2) libheif (decode-only, static) pointed at the staged libde265
    "$CMAKE_BIN" -S "$HEIF_SRC" -B "$WORK/heif-$ABI" -G Ninja \
        -DCMAKE_MAKE_PROGRAM="$NINJA_BIN" \
        -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN" \
        -DANDROID_ABI="$ABI" -DANDROID_PLATFORM="android-$ANDROID_API" \
        -DCMAKE_BUILD_TYPE=Release \
        -DBUILD_SHARED_LIBS=OFF \
        -DWITH_EXAMPLES=OFF -DWITH_GDK_PIXBUF=OFF -DBUILD_TESTING=OFF \
        -DWITH_AOM_DECODER=OFF -DWITH_AOM_ENCODER=OFF -DWITH_DAV1D=OFF \
        -DWITH_X265=OFF -DWITH_SvtEnc=OFF -DWITH_RAV1E=OFF \
        -DWITH_JPEG_DECODER=OFF -DWITH_JPEG_ENCODER=OFF \
        -DWITH_OpenJPEG_DECODER=OFF -DWITH_OpenJPEG_ENCODER=OFF \
        -DWITH_UNCOMPRESSED_CODEC=OFF \
        -DWITH_LIBDE265=ON -DENABLE_PLUGIN_LOADING=OFF \
        -DCMAKE_PREFIX_PATH="$STAGE" \
        -DCMAKE_FIND_ROOT_PATH="$STAGE" \
        -DLIBDE265_INCLUDE_DIR="$STAGE/include" \
        -DLIBDE265_LIBRARY="$STAGE/lib/libde265.a" \
        -DPKG_CONFIG_EXECUTABLE=/usr/bin/pkg-config \
        -DCMAKE_INSTALL_PREFIX="$STAGE"
    "$CMAKE_BIN" --build "$WORK/heif-$ABI" --target install

    # 3) Collect the static libs + headers we actually link into the JNI target.
    mkdir -p "$OUT/lib" "$OUT/include"
    cp -f "$STAGE/lib/libheif.a" "$OUT/lib/"
    cp -f "$STAGE/lib/libde265.a" "$OUT/lib/"
    cp -Rf "$STAGE/include/libheif" "$OUT/include/"
    echo "==> Installed $ABI: $(ls -1 "$OUT/lib")"
}

# --- ABI selection ---------------------------------------------------------------------------
# Accepts: no args (arm64-v8a), "all"/"universal" (all four), or one/more ABIs. Also accepts the
# capitalized CI flavor names (Arm64-v8a, Armeabi-v7a, X86_64, X86, Universal) so the workflow can
# pass ${{ matrix.arch }} directly.
declare -a ABIS=()
add_abi() {
    local a="$1"
    for existing in "${ABIS[@]:-}"; do [ "$existing" = "$a" ] && return; done
    ABIS+=("$a")
}
if [ "$#" -eq 0 ]; then
    add_abi "arm64-v8a"
else
    for arg in "$@"; do
        case "$(echo "$arg" | tr '[:upper:]' '[:lower:]')" in
            all|universal)   add_abi "arm64-v8a"; add_abi "armeabi-v7a"; add_abi "x86_64"; add_abi "x86" ;;
            arm64-v8a|arm64) add_abi "arm64-v8a" ;;
            armeabi-v7a|arm) add_abi "armeabi-v7a" ;;
            x86_64)          add_abi "x86_64" ;;
            x86)             add_abi "x86" ;;
            ci)              : ;;  # ignored marker
            *) echo "WARNING: unknown ABI arg '$arg', skipping" >&2 ;;
        esac
    done
fi
if [ "${#ABIS[@]}" -eq 0 ]; then add_abi "arm64-v8a"; fi

for abi in "${ABIS[@]}"; do
    build_abi "$abi"
done

echo "DONE. Prebuilt libs under: $OUT_ROOT"
