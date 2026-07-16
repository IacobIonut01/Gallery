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
