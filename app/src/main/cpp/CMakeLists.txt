# SPDX-FileCopyrightText: 2023-2026 IacobIacob01
# SPDX-License-Identifier: Apache-2.0
#
# Builds the `heiftiles` JNI shared library. It links the prebuilt static libheif + libde265
# (produced by scripts/native/build-heif.sh into heif/<ABI>/) to expose libheif's memory-bounded
# tiled decode. If no prebuilt exists for the current ABI, a stub .so is built instead and the
# Kotlin layer falls back to its non-native region-decode path.

cmake_minimum_required(VERSION 3.22.1)
project(heiftiles LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(HEIF_PREBUILT "${CMAKE_CURRENT_SOURCE_DIR}/heif/${ANDROID_ABI}")

add_library(heiftiles SHARED heif_tiles_jni.cpp)

find_library(log-lib log)

if(EXISTS "${HEIF_PREBUILT}/lib/libheif.a" AND EXISTS "${HEIF_PREBUILT}/lib/libde265.a")
    message(STATUS "heiftiles: using prebuilt libheif for ${ANDROID_ABI}")

    add_library(heif STATIC IMPORTED)
    set_target_properties(heif PROPERTIES
            IMPORTED_LOCATION "${HEIF_PREBUILT}/lib/libheif.a")

    add_library(de265 STATIC IMPORTED)
    set_target_properties(de265 PROPERTIES
            IMPORTED_LOCATION "${HEIF_PREBUILT}/lib/libde265.a")

    target_include_directories(heiftiles PRIVATE "${HEIF_PREBUILT}/include")
    target_compile_definitions(heiftiles PRIVATE HAVE_LIBHEIF=1)

    # Order matters for static linking: libheif references libde265 symbols, so heif before de265.
    target_link_libraries(heiftiles heif de265 ${log-lib})
else()
    message(WARNING "heiftiles: no prebuilt libheif for ${ANDROID_ABI}; building stub. "
            "Run scripts/native/build-heif.sh all to enable native tiled HEIC decode for every ABI.")
    target_link_libraries(heiftiles ${log-lib})
endif()

# ---------------------------------------------------------------------------------------------
# imgstream: scanline (row-streaming) JPEG + PNG encoder. Links prebuilt static libjpeg-turbo +
# libpng + zlib (produced by scripts/native/build-imgcodec.sh into imgcodec/<ABI>/). Lets the
# editor bake write a full-resolution result one strip at a time without holding the whole output
# bitmap. Missing prebuilt => stub, and Kotlin falls back to the whole-bitmap encode.
set(IMGCODEC_PREBUILT "${CMAKE_CURRENT_SOURCE_DIR}/imgcodec/${ANDROID_ABI}")

add_library(imgstream SHARED img_stream_jni.cpp)

if(EXISTS "${IMGCODEC_PREBUILT}/lib/libjpeg.a" AND EXISTS "${IMGCODEC_PREBUILT}/lib/libpng.a" AND EXISTS "${IMGCODEC_PREBUILT}/lib/libz.a")
    message(STATUS "imgstream: using prebuilt libjpeg-turbo + libpng for ${ANDROID_ABI}")

    add_library(jpeg STATIC IMPORTED)
    set_target_properties(jpeg PROPERTIES
            IMPORTED_LOCATION "${IMGCODEC_PREBUILT}/lib/libjpeg.a")

    add_library(png STATIC IMPORTED)
    set_target_properties(png PROPERTIES
            IMPORTED_LOCATION "${IMGCODEC_PREBUILT}/lib/libpng.a")

    add_library(z STATIC IMPORTED)
    set_target_properties(z PROPERTIES
            IMPORTED_LOCATION "${IMGCODEC_PREBUILT}/lib/libz.a")

    target_include_directories(imgstream PRIVATE "${IMGCODEC_PREBUILT}/include")
    target_compile_definitions(imgstream PRIVATE HAVE_IMGCODEC=1)

    # libpng depends on zlib, so png before z.
    target_link_libraries(imgstream jpeg png z ${log-lib})
else()
    message(WARNING "imgstream: no prebuilt libjpeg-turbo/libpng for ${ANDROID_ABI}; building stub. "
            "Run scripts/native/build-imgcodec.sh all to enable streaming JPEG/PNG encode for every ABI.")
    target_link_libraries(imgstream ${log-lib})
endif()

# ---------------------------------------------------------------------------------------------
# heifenc: tiled HEIC/AVIF encoder. Links prebuilt static libheif (WITH encoders) + x265 + aom +
# libde265 (produced by scripts/native/build-heif-encode.sh into heifenc/<ABI>/). Encodes a large
# edited image as a libheif grid — one tile at a time — so the whole decoded image is never held.
# Missing prebuilt => stub, and Kotlin falls back to the whole-bitmap HeifCoder encode.
set(HEIFENC_PREBUILT "${CMAKE_CURRENT_SOURCE_DIR}/heifenc/${ANDROID_ABI}")

add_library(heifenc SHARED heif_encode_jni.cpp)

if(EXISTS "${HEIFENC_PREBUILT}/lib/libheif.a" AND EXISTS "${HEIFENC_PREBUILT}/lib/libx265.a" AND EXISTS "${HEIFENC_PREBUILT}/lib/libaom.a")
    message(STATUS "heifenc: using prebuilt libheif (encoders) + x265 + aom for ${ANDROID_ABI}")

    add_library(heif_enc STATIC IMPORTED)
    set_target_properties(heif_enc PROPERTIES IMPORTED_LOCATION "${HEIFENC_PREBUILT}/lib/libheif.a")
    add_library(x265 STATIC IMPORTED)
    set_target_properties(x265 PROPERTIES IMPORTED_LOCATION "${HEIFENC_PREBUILT}/lib/libx265.a")
    add_library(aom STATIC IMPORTED)
    set_target_properties(aom PROPERTIES IMPORTED_LOCATION "${HEIFENC_PREBUILT}/lib/libaom.a")
    add_library(de265_enc STATIC IMPORTED)
    set_target_properties(de265_enc PROPERTIES IMPORTED_LOCATION "${HEIFENC_PREBUILT}/lib/libde265.a")

    target_include_directories(heifenc PRIVATE "${HEIFENC_PREBUILT}/include")
    target_compile_definitions(heifenc PRIVATE HAVE_HEIFENC=1)

    # libheif references x265/aom/de265 symbols, so libheif must come first. x265/aom are C++ and
    # pull in the STL; link it explicitly.
    target_link_libraries(heifenc heif_enc x265 aom de265_enc ${log-lib})
else()
    message(WARNING "heifenc: no prebuilt libheif-encoders/x265/aom for ${ANDROID_ABI}; building stub. "
            "Run scripts/native/build-heif-encode.sh all to enable tiled HEIC/AVIF encode for every ABI.")
    target_link_libraries(heifenc ${log-lib})
endif()
