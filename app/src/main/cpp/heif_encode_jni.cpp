/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 *
 * JNI bridge exposing libheif's TILED (grid-image) encode to Kotlin, so a full-resolution edited
 * image is encoded to HEIC (x265/HEVC) or AVIF (aom/AV1) one tile at a time — the whole decoded
 * image is never held in RAM; only the small compressed tiles accumulate. Tiles arrive from Kotlin
 * as packed ARGB_8888 ints (Bitmap.getPixels layout).
 *
 * When HAVE_HEIFENC is not defined (an ABI without prebuilt encoder libs) every function is a stub
 * reporting "unavailable" so the .so still loads and Kotlin falls back to the whole-bitmap encode.
 */

#include <jni.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <unistd.h>

#ifdef HAVE_HEIFENC
#include <android/log.h>
#include <libheif/heif.h>

#define LOG_TAG "HeifEncodeJni"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

struct HeifGridEncoder {
    heif_context* ctx = nullptr;
    heif_encoder* encoder = nullptr;
    heif_encoding_options* options = nullptr;
    heif_image_handle* grid = nullptr;
    FILE* file = nullptr;
    int tileWidth = 0;
    int tileHeight = 0;
};

heif_error writeToFile(heif_context*, const void* data, size_t size, void* userdata) {
    auto* f = static_cast<FILE*>(userdata);
    heif_error ok{ heif_error_Ok, heif_suberror_Unspecified, "" };
    heif_error fail{ heif_error_Encoding_error, heif_suberror_Unspecified, "write failed" };
    if (f == nullptr) return fail;
    if (size > 0 && fwrite(data, 1, size, f) != size) return fail;
    return ok;
}

} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeHeifEncoder_nativeSelfTest(JNIEnv*, jclass) {
    return JNI_TRUE;
}

// format: 0 = HEIC (HEVC/x265), 1 = AVIF (AV1/aom). Returns a handle or 0 on failure.
extern "C" JNIEXPORT jlong JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeHeifEncoder_nativeOpen(
        JNIEnv*, jclass, jint fd, jint width, jint height,
        jint tileWidth, jint tileHeight, jint format, jint quality) {
    if (width <= 0 || height <= 0 || tileWidth <= 0 || tileHeight <= 0) return 0;

    auto* enc = new HeifGridEncoder();
    enc->tileWidth = tileWidth;
    enc->tileHeight = tileHeight;

    int dupFd = dup(fd);
    if (dupFd < 0) { delete enc; return 0; }
    enc->file = fdopen(dupFd, "wb");
    if (enc->file == nullptr) { close(dupFd); delete enc; return 0; }

    enc->ctx = heif_context_alloc();
    const heif_compression_format comp =
            (format == 1) ? heif_compression_AV1 : heif_compression_HEVC;
    heif_error err = heif_context_get_encoder_for_format(enc->ctx, comp, &enc->encoder);
    if (err.code != heif_error_Ok || enc->encoder == nullptr) {
        LOGE("get_encoder_for_format(%d) failed: %s", format, err.message);
        heif_context_free(enc->ctx); fclose(enc->file); delete enc; return 0;
    }
    int q = quality; if (q < 0) q = 0; if (q > 100) q = 100;
    heif_encoder_set_lossy_quality(enc->encoder, q);

    enc->options = heif_encoding_options_alloc();

    const uint32_t columns = (static_cast<uint32_t>(width) + tileWidth - 1) / tileWidth;
    const uint32_t rows = (static_cast<uint32_t>(height) + tileHeight - 1) / tileHeight;
    err = heif_context_add_grid_image(
            enc->ctx, static_cast<uint32_t>(width), static_cast<uint32_t>(height),
            columns, rows, enc->options, &enc->grid);
    if (err.code != heif_error_Ok || enc->grid == nullptr) {
        LOGE("add_grid_image failed: %s", err.message);
        if (enc->options) heif_encoding_options_free(enc->options);
        heif_encoder_release(enc->encoder);
        heif_context_free(enc->ctx); fclose(enc->file); delete enc; return 0;
    }
    return reinterpret_cast<jlong>(enc);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeHeifEncoder_nativeEncodeTile(
        JNIEnv* env, jclass, jlong ptr, jintArray argb, jint tileW, jint tileH, jint tileX, jint tileY) {
    auto* enc = reinterpret_cast<HeifGridEncoder*>(ptr);
    if (enc == nullptr || tileW <= 0 || tileH <= 0) return JNI_FALSE;

    // ISO grid images require EVERY tile to be the same size (the grid cell size). Edge tiles on
    // the right/bottom are supplied smaller than the cell (tileW/tileH < enc->tileWidth/Height), so
    // encode them at the full cell size and clamp-replicate the interior pixels into the padding.
    // The grid's declared image_width/image_height crops the padding away on decode. Feeding a
    // smaller edge tile produces a malformed grid that no decoder can read.
    const int cellW = enc->tileWidth;
    const int cellH = enc->tileHeight;
    const int copyW = tileW < cellW ? tileW : cellW;
    const int copyH = tileH < cellH ? tileH : cellH;

    heif_image* img = nullptr;
    heif_error err = heif_image_create(cellW, cellH, heif_colorspace_RGB,
                                       heif_chroma_interleaved_RGBA, &img);
    if (err.code != heif_error_Ok || img == nullptr) return JNI_FALSE;
    err = heif_image_add_plane(img, heif_channel_interleaved, cellW, cellH, 8);
    if (err.code != heif_error_Ok) { heif_image_release(img); return JNI_FALSE; }

    int stride = 0;
    uint8_t* plane = heif_image_get_plane(img, heif_channel_interleaved, &stride);
    if (plane == nullptr) { heif_image_release(img); return JNI_FALSE; }

    jint* pixels = env->GetIntArrayElements(argb, nullptr);
    if (pixels == nullptr) { heif_image_release(img); return JNI_FALSE; }
    // The incoming buffer is packed at row stride == tileW.
    for (int y = 0; y < cellH; ++y) {
        const int sy = (y < copyH) ? y : (copyH - 1); // clamp to last valid row
        uint8_t* row = plane + static_cast<size_t>(y) * stride;
        const jint* src = pixels + static_cast<size_t>(sy) * tileW;
        for (int x = 0; x < cellW; ++x) {
            const int sx = (x < copyW) ? x : (copyW - 1); // clamp to last valid column
            const jint c = src[sx];
            row[x * 4 + 0] = static_cast<uint8_t>((c >> 16) & 0xFF); // R
            row[x * 4 + 1] = static_cast<uint8_t>((c >> 8) & 0xFF);  // G
            row[x * 4 + 2] = static_cast<uint8_t>(c & 0xFF);         // B
            row[x * 4 + 3] = static_cast<uint8_t>((c >> 24) & 0xFF); // A
        }
    }
    env->ReleaseIntArrayElements(argb, pixels, JNI_ABORT);

    err = heif_context_add_image_tile(enc->ctx, enc->grid,
                                      static_cast<uint32_t>(tileX), static_cast<uint32_t>(tileY),
                                      img, enc->encoder);
    heif_image_release(img);
    if (err.code != heif_error_Ok) {
        LOGE("add_image_tile(%d,%d) failed: %s", tileX, tileY, err.message);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeHeifEncoder_nativeFinish(
        JNIEnv*, jclass, jlong ptr) {
    auto* enc = reinterpret_cast<HeifGridEncoder*>(ptr);
    if (enc == nullptr) return JNI_FALSE;

    heif_writer writer{};
    writer.writer_api_version = 1;
    writer.write = writeToFile;
    heif_error err = heif_context_write(enc->ctx, &writer, enc->file);
    const bool ok = (err.code == heif_error_Ok);
    if (!ok) LOGE("context_write failed: %s", err.message);

    if (enc->options) heif_encoding_options_free(enc->options);
    if (enc->encoder) heif_encoder_release(enc->encoder);
    if (enc->ctx) heif_context_free(enc->ctx);
    if (enc->file) fclose(enc->file);
    delete enc;
    return ok ? JNI_TRUE : JNI_FALSE;
}

#else // !HAVE_HEIFENC — stubs.

extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeHeifEncoder_nativeSelfTest(JNIEnv*, jclass) { return JNI_FALSE; }
extern "C" JNIEXPORT jlong JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeHeifEncoder_nativeOpen(JNIEnv*, jclass, jint, jint, jint, jint, jint, jint, jint) { return 0; }
extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeHeifEncoder_nativeEncodeTile(JNIEnv*, jclass, jlong, jintArray, jint, jint, jint, jint) { return JNI_FALSE; }
extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeHeifEncoder_nativeFinish(JNIEnv*, jclass, jlong) { return JNI_FALSE; }

#endif // HAVE_HEIFENC
