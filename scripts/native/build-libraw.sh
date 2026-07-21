#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2023-2026 IacobIacob01
# SPDX-License-Identifier: Apache-2.0
#
# Cross-compiles LibRaw (+ libtiff, and the zlib/libjpeg-turbo they depend on) as static libraries
# for Android using the NDK CMake toolchain, installing the resulting static libs + headers into
#   app/src/main/cpp/rawcodec/<abi>/{include,lib}
# so the app's CMakeLists can link them into the `rawcodec` JNI library. That JNI bridge gives the
# app true native RAW decoding (demosaic) for every LibRaw-supported camera format, plus a 16-bit
# TIFF export path via libtiff.
#
# Run once per machine (and again only when bumping the pinned versions):
#   scripts/native/build-libraw.sh            # builds arm64-v8a (device/dev ABI)
#   scripts/native/build-libraw.sh all        # builds all four ABIs for release
#
# Requires: internet access, the pinned NDK, and SDK CMake 3.31.x.
set -euo pipefail

# --- Pinned versions -------------------------------------------------------------------------
ZLIB_VERSION="1.3.1"
LIBJPEGTURBO_VERSION="3.0.4"
LIBRAW_VERSION="0.21.3"
LIBTIFF_VERSION="4.6.0"
ANDROID_API=29                          # matches app minSdk
CMAKE_VERSION="3.31.6"

# --- Paths -----------------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
OUT_ROOT="$REPO_ROOT/app/src/main/cpp/rawcodec"

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

# --- Source download -------------------------------------------------------------------------
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
ZLIB_SRC="$WORK/zlib-$ZLIB_VERSION"
JPEG_SRC="$WORK/libjpeg-turbo-$LIBJPEGTURBO_VERSION"
RAW_SRC="$WORK/LibRaw-$LIBRAW_VERSION"
TIFF_SRC="$WORK/tiff-$LIBTIFF_VERSION"

echo "==> Downloading zlib $ZLIB_VERSION"
curl -fsSL "https://github.com/madler/zlib/releases/download/v$ZLIB_VERSION/zlib-$ZLIB_VERSION.tar.gz" \
    | tar -xz -C "$WORK"
echo "==> Downloading libjpeg-turbo $LIBJPEGTURBO_VERSION"
curl -fsSL "https://github.com/libjpeg-turbo/libjpeg-turbo/releases/download/$LIBJPEGTURBO_VERSION/libjpeg-turbo-$LIBJPEGTURBO_VERSION.tar.gz" \
    | tar -xz -C "$WORK"
echo "==> Downloading LibRaw $LIBRAW_VERSION"
curl -fsSL "https://www.libraw.org/data/LibRaw-$LIBRAW_VERSION.tar.gz" | tar -xz -C "$WORK"
echo "==> Downloading libtiff $LIBTIFF_VERSION"
curl -fsSL "https://download.osgeo.org/libtiff/tiff-$LIBTIFF_VERSION.tar.gz" | tar -xz -C "$WORK"

build_abi() {
    local ABI="$1"
    echo "================================================================"
    echo "==> Building rawcodec deps for $ABI"
    echo "================================================================"
    local STAGE="$WORK/stage-$ABI"
    local OUT="$OUT_ROOT/$ABI"
    rm -rf "$OUT"
    mkdir -p "$OUT/lib" "$OUT/include"

    local COMMON=(
        -G Ninja
        -DCMAKE_MAKE_PROGRAM="$NINJA_BIN"
        -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN"
        -DANDROID_ABI="$ABI" -DANDROID_PLATFORM="android-$ANDROID_API"
        -DCMAKE_BUILD_TYPE=Release
        -DBUILD_SHARED_LIBS=OFF
        -DCMAKE_INSTALL_PREFIX="$STAGE"
        -DCMAKE_FIND_ROOT_PATH="$STAGE"
        -DCMAKE_PREFIX_PATH="$STAGE"
    )

    # 1) zlib (needed by libtiff deflate + LibRaw deflated DNG)
    "$CMAKE_BIN" -S "$ZLIB_SRC" -B "$WORK/zlib-$ABI" "${COMMON[@]}"
    "$CMAKE_BIN" --build "$WORK/zlib-$ABI" --target install
    rm -f "$STAGE/lib/libz.so"* 2>/dev/null || true

    # 2) libjpeg-turbo (classic libjpeg API; used by LibRaw lossy-DNG + JPEG-in-TIFF)
    "$CMAKE_BIN" -S "$JPEG_SRC" -B "$WORK/jpeg-$ABI" "${COMMON[@]}" \
        -DENABLE_SHARED=OFF -DENABLE_STATIC=ON \
        -DWITH_TURBOJPEG=OFF -DWITH_JPEG8=ON
    "$CMAKE_BIN" --build "$WORK/jpeg-$ABI" --target install

    # 3) libtiff (static; deflate via staged zlib, jpeg via staged libjpeg)
    "$CMAKE_BIN" -S "$TIFF_SRC" -B "$WORK/tiff-$ABI" "${COMMON[@]}" \
        -Dtiff-tools=OFF -Dtiff-tests=OFF -Dtiff-contrib=OFF -Dtiff-docs=OFF \
        -Dlzma=OFF -Dzstd=OFF -Dwebp=OFF -Djbig=OFF -Dlerc=OFF \
        -Dzlib=ON -Djpeg=ON \
        -DZLIB_INCLUDE_DIR="$STAGE/include" -DZLIB_LIBRARY="$STAGE/lib/libz.a" \
        -DJPEG_INCLUDE_DIR="$STAGE/include" -DJPEG_LIBRARY="$STAGE/lib/libjpeg.a"
    "$CMAKE_BIN" --build "$WORK/tiff-$ABI" --target install

    # 4) LibRaw (static). The libraw.org tarball has NO CMakeLists.txt — it ships autotools +
    #    Makefile.dist. We drive Makefile.dist's `library` target with the NDK clang, baking every
    #    needed define into CFLAGS (a make-command-line CFLAGS= overrides the Makefile's `CFLAGS+=`
    #    lines, so USE_ZLIB/USE_JPEG must be included here). LDADD is irrelevant when only archiving
    #    the static lib. Makefile.dist archives with bare `ar`/`ranlib`, so we shim those to the NDK
    #    llvm tools on PATH (host ranlib can't index cross-compiled ELF objects).
    local HOST_TAG CCB
    HOST_TAG="$(ls -d "$NDK_DIR"/toolchains/llvm/prebuilt/*/bin 2>/dev/null | head -n1)"
    case "$ABI" in
        arm64-v8a)   CCB="aarch64-linux-android$ANDROID_API" ;;
        armeabi-v7a) CCB="armv7a-linux-androideabi$ANDROID_API" ;;
        x86_64)      CCB="x86_64-linux-android$ANDROID_API" ;;
        x86)         CCB="i686-linux-android$ANDROID_API" ;;
        *) echo "ERROR: unmapped ABI '$ABI' for LibRaw" >&2; return 1 ;;
    esac
    if [ -z "$HOST_TAG" ] || [ ! -x "$HOST_TAG/$CCB-clang" ]; then
        echo "ERROR: NDK clang '$CCB-clang' not found under $HOST_TAG" >&2; return 1
    fi

    local SHIM="$WORK/shim-$ABI"
    mkdir -p "$SHIM"
    ln -sf "$HOST_TAG/llvm-ar" "$SHIM/ar"
    ln -sf "$HOST_TAG/llvm-ranlib" "$SHIM/ranlib"

    # Build in a per-ABI copy: Makefile.dist writes object/ + lib/ inside the source tree.
    local RAWB="$WORK/LibRaw-$ABI-src"
    rm -rf "$RAWB"; cp -R "$RAW_SRC" "$RAWB"
    mkdir -p "$RAWB/object" "$RAWB/lib" "$RAWB/bin"
    local JOBS; JOBS="$(sysctl -n hw.ncpu 2>/dev/null || echo 4)"
    (
        cd "$RAWB"
        PATH="$SHIM:$PATH" make -f Makefile.dist -j"$JOBS" library \
            CC="$HOST_TAG/$CCB-clang" \
            CXX="$HOST_TAG/$CCB-clang++" \
            CFLAGS="-O3 -I. -w -fPIC -DNDEBUG -DUSE_ZLIB -DUSE_JPEG -DUSE_JPEG8 -I$STAGE/include"
    )

    # Stage LibRaw's headers + static libs where step 5 (and the app CMake) expect them.
    mkdir -p "$STAGE/include/libraw"
    cp -f "$RAWB"/libraw/*.h "$STAGE/include/libraw/"
    cp -f "$RAWB"/lib/libraw.a "$STAGE/lib/" 2>/dev/null || true
    cp -f "$RAWB"/lib/libraw_r.a "$STAGE/lib/" 2>/dev/null || true

    # 5) Collect the static libs + headers the JNI target links.
    cp -f "$STAGE/lib/libz.a" "$OUT/lib/"
    cp -f "$STAGE/lib/libjpeg.a" "$OUT/lib/"
    if [ -f "$STAGE/lib/libtiff.a" ]; then cp -f "$STAGE/lib/libtiff.a" "$OUT/lib/"; fi
    # LibRaw installs libraw.a (non-thread-safe) and libraw_r.a (reentrant); prefer libraw_r.
    if [ -f "$STAGE/lib/libraw_r.a" ]; then cp -f "$STAGE/lib/libraw_r.a" "$OUT/lib/libraw.a"; \
    elif [ -f "$STAGE/lib/libraw.a" ]; then cp -f "$STAGE/lib/libraw.a" "$OUT/lib/libraw.a"; fi

    cp -f "$STAGE/include/zlib.h" "$STAGE/include/zconf.h" "$OUT/include/" 2>/dev/null || true
    cp -f "$STAGE/include/jpeglib.h" "$STAGE/include/jconfig.h" "$STAGE/include/jmorecfg.h" "$OUT/include/" 2>/dev/null || true
    cp -f "$STAGE"/include/tiff*.h "$OUT/include/" 2>/dev/null || true
    mkdir -p "$OUT/include/libraw"
    cp -f "$STAGE"/include/libraw/*.h "$OUT/include/libraw/" 2>/dev/null || true
    echo "==> Installed $ABI: $(ls -1 "$OUT/lib")"
}

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
            ci)              : ;;
            *) echo "WARNING: unknown ABI arg '$arg', skipping" >&2 ;;
        esac
    done
fi
if [ "${#ABIS[@]}" -eq 0 ]; then add_abi "arm64-v8a"; fi

for abi in "${ABIS[@]}"; do
    build_abi "$abi"
done

echo "DONE. Prebuilt libs under: $OUT_ROOT"
