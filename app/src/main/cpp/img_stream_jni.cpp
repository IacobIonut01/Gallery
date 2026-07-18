/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 *
 * JNI bridge exposing scanline (row-streaming) JPEG + PNG encoders backed by the prebuilt static
 * libjpeg-turbo + libpng (produced by scripts/native/build-imgcodec.sh). The editor's tiled bake
 * feeds the full-resolution result one horizontal strip at a time, so the whole output image is
 * never materialised in RAM — enabling saves of arbitrarily large edited images without OOM.
 *
 * Rows arrive from Kotlin as packed ARGB_8888 ints (Bitmap.getPixels layout). When HAVE_IMGCODEC
 * is not defined (an ABI without prebuilt libs) every function is a stub reporting "unavailable" so
 * the .so still loads and Kotlin falls back to the whole-bitmap encode.
 */

#include <jni.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <unistd.h>

#ifdef HAVE_IMGCODEC
#include <android/log.h>
#include <csetjmp>
#include <jpeglib.h>
#include <png.h>

#define LOG_TAG "ImgStreamJni"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

struct JpegEncoder {
    FILE* file = nullptr;
    jpeg_compress_struct cinfo{};
    jpeg_error_mgr jerr{};
    int width = 0;
    bool started = false;
};

struct PngEncoder {
    FILE* file = nullptr;
    png_structp png = nullptr;
    png_infop info = nullptr;
    int width = 0;
};

// Opens a buffered stdio stream on a dup() of the Java-owned fd so the native encoder can fclose
// its own copy without disturbing the ParcelFileDescriptor the Kotlin side still owns.
FILE* fdopenDup(int fd, const char* mode) {
    int dup_fd = dup(fd);
    if (dup_fd < 0) return nullptr;
    FILE* f = fdopen(dup_fd, mode);
    if (f == nullptr) close(dup_fd);
    return f;
}

} // namespace

// ============================== self test ==============================
extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeImageEncoder_nativeSelfTest(
        JNIEnv*, jclass) {
    return JNI_TRUE;
}

// ============================== JPEG ==============================
extern "C" JNIEXPORT jlong JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeImageEncoder_nativeJpegOpen(
        JNIEnv*, jclass, jint fd, jint width, jint height, jint quality) {
    if (width <= 0 || height <= 0) return 0;
    auto* enc = new JpegEncoder();
    enc->width = width;
    enc->file = fdopenDup(fd, "wb");
    if (enc->file == nullptr) { delete enc; return 0; }

    enc->cinfo.err = jpeg_std_error(&enc->jerr);
    jpeg_create_compress(&enc->cinfo);
    jpeg_stdio_dest(&enc->cinfo, enc->file);
    enc->cinfo.image_width = static_cast<JDIMENSION>(width);
    enc->cinfo.image_height = static_cast<JDIMENSION>(height);
    enc->cinfo.input_components = 3;
    enc->cinfo.in_color_space = JCS_RGB;
    jpeg_set_defaults(&enc->cinfo);
    int q = quality; if (q < 1) q = 1; if (q > 100) q = 100;
    jpeg_set_quality(&enc->cinfo, q, TRUE);
    jpeg_start_compress(&enc->cinfo, TRUE);
    enc->started = true;
    return reinterpret_cast<jlong>(enc);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeImageEncoder_nativeJpegWriteRows(
        JNIEnv* env, jclass, jlong ptr, jintArray argb, jint rowCount) {
    auto* enc = reinterpret_cast<JpegEncoder*>(ptr);
    if (enc == nullptr || rowCount <= 0) return JNI_FALSE;
    const int w = enc->width;
    jint* pixels = env->GetIntArrayElements(argb, nullptr);
    if (pixels == nullptr) return JNI_FALSE;

    auto* row = static_cast<JSAMPLE*>(malloc(static_cast<size_t>(w) * 3));
    if (row == nullptr) { env->ReleaseIntArrayElements(argb, pixels, JNI_ABORT); return JNI_FALSE; }

    jboolean ok = JNI_TRUE;
    for (int y = 0; y < rowCount; ++y) {
        const jint* src = pixels + static_cast<size_t>(y) * w;
        for (int x = 0; x < w; ++x) {
            const jint c = src[x];
            row[x * 3 + 0] = static_cast<JSAMPLE>((c >> 16) & 0xFF); // R
            row[x * 3 + 1] = static_cast<JSAMPLE>((c >> 8) & 0xFF);  // G
            row[x * 3 + 2] = static_cast<JSAMPLE>(c & 0xFF);         // B
        }
        JSAMPROW rp = row;
        if (jpeg_write_scanlines(&enc->cinfo, &rp, 1) != 1) { ok = JNI_FALSE; break; }
    }
    free(row);
    env->ReleaseIntArrayElements(argb, pixels, JNI_ABORT);
    return ok;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeImageEncoder_nativeJpegFinish(
        JNIEnv*, jclass, jlong ptr) {
    auto* enc = reinterpret_cast<JpegEncoder*>(ptr);
    if (enc == nullptr) return JNI_FALSE;
    if (enc->started) jpeg_finish_compress(&enc->cinfo);
    jpeg_destroy_compress(&enc->cinfo);
    if (enc->file != nullptr) fclose(enc->file);
    delete enc;
    return JNI_TRUE;
}

// ============================== PNG ==============================
extern "C" JNIEXPORT jlong JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeImageEncoder_nativePngOpen(
        JNIEnv*, jclass, jint fd, jint width, jint height) {
    if (width <= 0 || height <= 0) return 0;
    auto* enc = new PngEncoder();
    enc->width = width;
    enc->file = fdopenDup(fd, "wb");
    if (enc->file == nullptr) { delete enc; return 0; }

    enc->png = png_create_write_struct(PNG_LIBPNG_VER_STRING, nullptr, nullptr, nullptr);
    if (enc->png == nullptr) { fclose(enc->file); delete enc; return 0; }
    enc->info = png_create_info_struct(enc->png);
    if (enc->info == nullptr) {
        png_destroy_write_struct(&enc->png, nullptr);
        fclose(enc->file); delete enc; return 0;
    }
    if (setjmp(png_jmpbuf(enc->png))) {
        png_destroy_write_struct(&enc->png, &enc->info);
        fclose(enc->file); delete enc; return 0;
    }
    png_init_io(enc->png, enc->file);
    png_set_IHDR(enc->png, enc->info,
                 static_cast<png_uint_32>(width), static_cast<png_uint_32>(height),
                 8, PNG_COLOR_TYPE_RGBA, PNG_INTERLACE_NONE,
                 PNG_COMPRESSION_TYPE_DEFAULT, PNG_FILTER_TYPE_DEFAULT);
    png_write_info(enc->png, enc->info);
    return reinterpret_cast<jlong>(enc);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeImageEncoder_nativePngWriteRows(
        JNIEnv* env, jclass, jlong ptr, jintArray argb, jint rowCount) {
    auto* enc = reinterpret_cast<PngEncoder*>(ptr);
    if (enc == nullptr || rowCount <= 0) return JNI_FALSE;
    const int w = enc->width;
    jint* pixels = env->GetIntArrayElements(argb, nullptr);
    if (pixels == nullptr) return JNI_FALSE;

    auto* row = static_cast<png_bytep>(malloc(static_cast<size_t>(w) * 4));
    if (row == nullptr) { env->ReleaseIntArrayElements(argb, pixels, JNI_ABORT); return JNI_FALSE; }

    jboolean ok = JNI_TRUE;
    if (setjmp(png_jmpbuf(enc->png))) {
        free(row);
        env->ReleaseIntArrayElements(argb, pixels, JNI_ABORT);
        return JNI_FALSE;
    }
    for (int y = 0; y < rowCount; ++y) {
        const jint* src = pixels + static_cast<size_t>(y) * w;
        for (int x = 0; x < w; ++x) {
            const jint c = src[x];
            row[x * 4 + 0] = static_cast<png_byte>((c >> 16) & 0xFF); // R
            row[x * 4 + 1] = static_cast<png_byte>((c >> 8) & 0xFF);  // G
            row[x * 4 + 2] = static_cast<png_byte>(c & 0xFF);         // B
            row[x * 4 + 3] = static_cast<png_byte>((c >> 24) & 0xFF); // A
        }
        png_write_row(enc->png, row);
    }
    free(row);
    env->ReleaseIntArrayElements(argb, pixels, JNI_ABORT);
    return ok;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeImageEncoder_nativePngFinish(
        JNIEnv*, jclass, jlong ptr) {
    auto* enc = reinterpret_cast<PngEncoder*>(ptr);
    if (enc == nullptr) return JNI_FALSE;
    if (setjmp(png_jmpbuf(enc->png))) {
        png_destroy_write_struct(&enc->png, &enc->info);
        if (enc->file != nullptr) fclose(enc->file);
        delete enc;
        return JNI_FALSE;
    }
    png_write_end(enc->png, enc->info);
    png_destroy_write_struct(&enc->png, &enc->info);
    if (enc->file != nullptr) fclose(enc->file);
    delete enc;
    return JNI_TRUE;
}

#else // !HAVE_IMGCODEC — stubs so the .so still loads and Kotlin falls back.

extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeImageEncoder_nativeSelfTest(JNIEnv*, jclass) { return JNI_FALSE; }
extern "C" JNIEXPORT jlong JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeImageEncoder_nativeJpegOpen(JNIEnv*, jclass, jint, jint, jint, jint) { return 0; }
extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeImageEncoder_nativeJpegWriteRows(JNIEnv*, jclass, jlong, jintArray, jint) { return JNI_FALSE; }
extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeImageEncoder_nativeJpegFinish(JNIEnv*, jclass, jlong) { return JNI_FALSE; }
extern "C" JNIEXPORT jlong JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeImageEncoder_nativePngOpen(JNIEnv*, jclass, jint, jint, jint) { return 0; }
extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeImageEncoder_nativePngWriteRows(JNIEnv*, jclass, jlong, jintArray, jint) { return JNI_FALSE; }
extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_feature_1node_presentation_edit_bake_NativeImageEncoder_nativePngFinish(JNIEnv*, jclass, jlong) { return JNI_FALSE; }

#endif // HAVE_IMGCODEC
