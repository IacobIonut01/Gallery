/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 *
 * JNI bridge exposing libheif's memory-bounded tiled decode to Kotlin. Enables crisp zoom of very
 * large (e.g. 100MP) HEIC/HEIF images without decoding the whole frame: only the requested grid
 * tiles are decoded, each at native resolution.
 *
 * When HAVE_LIBHEIF is not defined (an ABI without prebuilt libs), the functions are compiled as
 * stubs that report "unavailable" so the Kotlin layer transparently falls back to its non-native
 * region-decode path.
 */

#include <jni.h>
#include <cstdlib>
#include <cstring>

#ifdef HAVE_LIBHEIF
#include <android/log.h>
#include <libheif/heif.h>

#define LOG_TAG "HeifTilesJni"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

// Holds an open libheif context + primary handle + cached tiling for one image.
struct HeifTiler {
    heif_context* ctx = nullptr;
    heif_image_handle* handle = nullptr;
    heif_image_tiling tiling{};
    // libheif reads without copying, so the source bytes must outlive the context.
    uint8_t* data = nullptr;
    size_t dataSize = 0;
};

// Process image transformations (rotation/mirror/crop) internally so tile coordinates match the
// displayed geometry. Must be paired with ignore_transformations = false on decode.
constexpr int kProcessTransforms = 1;

} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_core_decoder_NativeHeifTiler_nativeSelfTest(JNIEnv*, jclass) {
    return JNI_TRUE;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_dot_gallery_core_decoder_NativeHeifTiler_nativeOpen(JNIEnv* env, jclass, jbyteArray data) {
    if (data == nullptr) return 0;
    const jsize size = env->GetArrayLength(data);
    if (size <= 0) return 0;

    auto* tiler = new HeifTiler();
    tiler->dataSize = static_cast<size_t>(size);
    tiler->data = static_cast<uint8_t*>(malloc(tiler->dataSize));
    if (tiler->data == nullptr) {
        delete tiler;
        return 0;
    }
    env->GetByteArrayRegion(data, 0, size, reinterpret_cast<jbyte*>(tiler->data));

    tiler->ctx = heif_context_alloc();
    heif_error err = heif_context_read_from_memory_without_copy(
            tiler->ctx, tiler->data, tiler->dataSize, nullptr);
    if (err.code != heif_error_Ok) {
        LOGE("read_from_memory failed: %s", err.message);
        heif_context_free(tiler->ctx);
        free(tiler->data);
        delete tiler;
        return 0;
    }

    err = heif_context_get_primary_image_handle(tiler->ctx, &tiler->handle);
    if (err.code != heif_error_Ok || tiler->handle == nullptr) {
        LOGE("get_primary_image_handle failed: %s", err.message);
        heif_context_free(tiler->ctx);
        free(tiler->data);
        delete tiler;
        return 0;
    }

    err = heif_image_handle_get_image_tiling(tiler->handle, kProcessTransforms, &tiler->tiling);
    if (err.code != heif_error_Ok) {
        LOGE("get_image_tiling failed: %s", err.message);
        heif_image_handle_release(tiler->handle);
        heif_context_free(tiler->ctx);
        free(tiler->data);
        delete tiler;
        return 0;
    }

    return reinterpret_cast<jlong>(tiler);
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_dot_gallery_core_decoder_NativeHeifTiler_nativeGetInfo(JNIEnv* env, jclass, jlong ptr) {
    auto* tiler = reinterpret_cast<HeifTiler*>(ptr);
    if (tiler == nullptr) return nullptr;
    int lumaBits = heif_image_handle_get_luma_bits_per_pixel(tiler->handle);
    if (lumaBits <= 0) lumaBits = 8;
    jint info[7] = {
            static_cast<jint>(tiler->tiling.image_width),
            static_cast<jint>(tiler->tiling.image_height),
            static_cast<jint>(tiler->tiling.tile_width),
            static_cast<jint>(tiler->tiling.tile_height),
            static_cast<jint>(tiler->tiling.num_columns),
            static_cast<jint>(tiler->tiling.num_rows),
            static_cast<jint>(lumaBits),
    };
    jintArray out = env->NewIntArray(7);
    if (out == nullptr) return nullptr;
    env->SetIntArrayRegion(out, 0, 7, info);
    return out;
}

// Returns an int[] laid out as [width, height, pixels...] where pixels are packed ARGB_8888,
// or null on failure. Decodes exactly one grid tile at native resolution (bounded memory).
extern "C" JNIEXPORT jintArray JNICALL
Java_com_dot_gallery_core_decoder_NativeHeifTiler_nativeDecodeTile(
        JNIEnv* env, jclass, jlong ptr, jint tileX, jint tileY) {
    auto* tiler = reinterpret_cast<HeifTiler*>(ptr);
    if (tiler == nullptr) return nullptr;

    heif_decoding_options* options = heif_decoding_options_alloc();
    if (options != nullptr) {
        // Matches kProcessTransforms=1 used for tiling coordinates.
        options->ignore_transformations = 0;
    }

    heif_image* img = nullptr;
    heif_error err = heif_image_handle_decode_image_tile(
            tiler->handle, &img,
            heif_colorspace_RGB, heif_chroma_interleaved_RGBA,
            options,
            static_cast<uint32_t>(tileX), static_cast<uint32_t>(tileY));
    if (options != nullptr) heif_decoding_options_free(options);

    if (err.code != heif_error_Ok || img == nullptr) {
        LOGE("decode_image_tile(%d,%d) failed: %s", tileX, tileY, err.message);
        return nullptr;
    }

    const int w = heif_image_get_width(img, heif_channel_interleaved);
    const int h = heif_image_get_height(img, heif_channel_interleaved);
    if (w <= 0 || h <= 0) {
        heif_image_release(img);
        return nullptr;
    }

    int stride = 0;
    const uint8_t* plane = heif_image_get_plane_readonly(img, heif_channel_interleaved, &stride);
    if (plane == nullptr) {
        heif_image_release(img);
        return nullptr;
    }

    const jsize total = static_cast<jsize>(w) * static_cast<jsize>(h) + 2;
    jintArray out = env->NewIntArray(total);
    if (out == nullptr) {
        heif_image_release(img);
        return nullptr;
    }

    jint* buf = env->GetIntArrayElements(out, nullptr);
    if (buf == nullptr) {
        heif_image_release(img);
        return nullptr;
    }
    buf[0] = w;
    buf[1] = h;
    // Repack interleaved RGBA (byte order R,G,B,A) into packed ARGB_8888 ints.
    jint* dst = buf + 2;
    for (int y = 0; y < h; ++y) {
        const uint8_t* row = plane + static_cast<size_t>(y) * stride;
        for (int x = 0; x < w; ++x) {
            const uint8_t r = row[x * 4 + 0];
            const uint8_t g = row[x * 4 + 1];
            const uint8_t b = row[x * 4 + 2];
            const uint8_t a = row[x * 4 + 3];
            dst[y * w + x] = (static_cast<jint>(a) << 24) |
                             (static_cast<jint>(r) << 16) |
                             (static_cast<jint>(g) << 8) |
                             (static_cast<jint>(b));
        }
    }
    env->ReleaseIntArrayElements(out, buf, 0);
    heif_image_release(img);
    return out;
}

extern "C" JNIEXPORT void JNICALL
Java_com_dot_gallery_core_decoder_NativeHeifTiler_nativeClose(JNIEnv*, jclass, jlong ptr) {
    auto* tiler = reinterpret_cast<HeifTiler*>(ptr);
    if (tiler == nullptr) return;
    if (tiler->handle != nullptr) heif_image_handle_release(tiler->handle);
    if (tiler->ctx != nullptr) heif_context_free(tiler->ctx);
    if (tiler->data != nullptr) free(tiler->data);
    delete tiler;
}

#else // !HAVE_LIBHEIF — stubs so the .so still loads and the Kotlin layer falls back.

extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_core_decoder_NativeHeifTiler_nativeSelfTest(JNIEnv*, jclass) {
    return JNI_FALSE;
}
extern "C" JNIEXPORT jlong JNICALL
Java_com_dot_gallery_core_decoder_NativeHeifTiler_nativeOpen(JNIEnv*, jclass, jbyteArray) {
    return 0;
}
extern "C" JNIEXPORT jintArray JNICALL
Java_com_dot_gallery_core_decoder_NativeHeifTiler_nativeGetInfo(JNIEnv*, jclass, jlong) {
    return nullptr;
}
extern "C" JNIEXPORT jintArray JNICALL
Java_com_dot_gallery_core_decoder_NativeHeifTiler_nativeDecodeTile(JNIEnv*, jclass, jlong, jint, jint) {
    return nullptr;
}
extern "C" JNIEXPORT void JNICALL
Java_com_dot_gallery_core_decoder_NativeHeifTiler_nativeClose(JNIEnv*, jclass, jlong) {
}

#endif // HAVE_LIBHEIF
