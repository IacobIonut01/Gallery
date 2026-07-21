/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 *
 * JNI bridge exposing LibRaw's true RAW decode (demosaic) + a libtiff 8/16-bit TIFF writer to
 * Kotlin. Gives the app native full-resolution RAW rendering (with white balance / exposure /
 * highlight / colour-space / demosaic controls) for every LibRaw-supported camera format, and a
 * develop-to-TIFF export path.
 *
 * When HAVE_LIBRAW is not defined (an ABI without prebuilt libs) every function is compiled as a
 * stub reporting "unavailable" so the .so still loads and the Kotlin layer transparently falls
 * back to the embedded-JPEG-preview path.
 */

#include <jni.h>
#include <cstdlib>
#include <cstring>
#include <unistd.h>

#ifdef HAVE_LIBRAW
#include <cmath>
#include <android/log.h>
#include <libraw/libraw.h>
#include <tiffio.h>

#define LOG_TAG "RawCodecJni"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

// ── Fixed layouts shared with NativeRawDecoder.kt ─────────────────────────────
// intParams[]:  0=useCameraWb 1=useAutoWb 2=highlight 3=outputColor 4=quality
//               5=halfSize 6=userFlip(-1=meta) 7=fbddNoiseRd 8=outputBps
// floatParams[]:0=exposureEv 1=brightness 2=contrast 3=saturation 4=vibrance
//               5=shadows 6=highlightsTone 7=sharpen 8=nrThreshold
enum { I_CAMWB=0, I_AUTOWB, I_HIGHLIGHT, I_OUTCOLOR, I_QUAL, I_HALF, I_FLIP, I_FBDD, I_BPS, I_COUNT };
enum { F_EXPOSURE=0, F_BRIGHT, F_CONTRAST, F_SAT, F_VIB, F_SHADOWS, F_HIGHLIGHTS, F_SHARPEN, F_NRTHRESH, F_COUNT };

struct DevelopParams {
    jint ip[I_COUNT];
    jfloat fp[F_COUNT];
    jfloat mul[4];
    bool hasMul = false;
};

DevelopParams readParams(JNIEnv* env, jintArray intArr, jfloatArray floatArr, jfloatArray userMulArr) {
    DevelopParams d{};
    for (int i = 0; i < I_COUNT; ++i) d.ip[i] = 0;
    for (int i = 0; i < F_COUNT; ++i) d.fp[i] = 0.0f;
    d.ip[I_FLIP] = -1;
    if (intArr != nullptr) {
        jsize n = env->GetArrayLength(intArr);
        env->GetIntArrayRegion(intArr, 0, n < I_COUNT ? n : I_COUNT, d.ip);
    }
    if (floatArr != nullptr) {
        jsize n = env->GetArrayLength(floatArr);
        env->GetFloatArrayRegion(floatArr, 0, n < F_COUNT ? n : F_COUNT, d.fp);
    }
    if (userMulArr != nullptr && env->GetArrayLength(userMulArr) >= 4) {
        env->GetFloatArrayRegion(userMulArr, 0, 4, d.mul);
        d.hasMul = true;
    }
    return d;
}

// Applies the LibRaw-native develop parameters onto a freshly-opened LibRaw instance.
void applyParams(LibRaw& raw, const DevelopParams& d, jint outputBps) {
    auto& p = raw.imgdata.params;
    p.use_camera_wb = d.ip[I_CAMWB] ? 1 : 0;
    p.use_auto_wb = d.ip[I_AUTOWB] ? 1 : 0;
    if (d.hasMul) for (int i = 0; i < 4; ++i) p.user_mul[i] = d.mul[i];
    p.output_color = d.ip[I_OUTCOLOR];   // 0=raw,1=sRGB,2=Adobe,3=Wide,4=ProPhoto,5=XYZ
    p.user_qual = d.ip[I_QUAL];          // 0=linear,1=VNG,2=PPG,3=AHD,4=DCB
    p.half_size = d.ip[I_HALF] ? 1 : 0;
    p.highlight = d.ip[I_HIGHLIGHT];     // 0=clip,1=unclip,2=blend,3..9=rebuild
    p.output_bps = outputBps;            // 8 or 16
    p.no_auto_bright = 1;                // deterministic output; exposure driven by exp_shift
    p.gamm[0] = 1.0 / 2.4;               // sRGB-ish gamma
    p.gamm[1] = 12.92;
    if (d.ip[I_FLIP] >= 0) p.user_flip = d.ip[I_FLIP];   // EXIF-derived orientation override
    if (d.ip[I_FBDD] > 0) p.fbdd_noiserd = d.ip[I_FBDD]; // 1=light,2=full FBDD denoise
    if (d.fp[F_NRTHRESH] > 0.0f) p.threshold = d.fp[F_NRTHRESH]; // wavelet denoise strength
    const float bright = d.fp[F_BRIGHT];
    if (bright != 0.0f) p.bright = std::pow(2.0f, bright); // brightness in stops
    if (d.fp[F_EXPOSURE] != 0.0f) {
        p.exp_correc = 1;
        p.exp_shift = static_cast<float>(std::pow(2.0, static_cast<double>(d.fp[F_EXPOSURE])));
        p.exp_preser = 1.0f;             // preserve highlights during exposure shift
    }
}

// ── Post-demosaic tone stage (shared by preview + export) ─────────────────────
// Templated core so it can run over either the LibRaw interleaved RGB buffer
// (8/16-bit, export) or a packed ARGB buffer (8-bit, live preview) via getv/setv
// accessors indexed by RGB-interleaved offset (pixel*3 + channel). Every op is a
// no-op at its neutral (0) default, keeping preview and export WYSIWYG-identical.
template <typename Get, typename Set>
static void applyToneCore(int w, int h, const DevelopParams& d, Get getv, Set setv) {
    const float contrast = d.fp[F_CONTRAST];
    const float sat = d.fp[F_SAT];
    const float vib = d.fp[F_VIB];
    const float shadows = d.fp[F_SHADOWS];
    const float highlights = d.fp[F_HIGHLIGHTS];
    const float sharpen = d.fp[F_SHARPEN];
    const bool anyTone = contrast != 0.0f || sat != 0.0f || vib != 0.0f ||
                         shadows != 0.0f || highlights != 0.0f;
    if (!anyTone && sharpen == 0.0f) return;

    const size_t px = static_cast<size_t>(w) * static_cast<size_t>(h);

    if (anyTone) {
        const float cFactor = 1.0f + contrast; // contrast in [-1,1] → factor [0,2]
        for (size_t i = 0; i < px; ++i) {
            float r = getv(i * 3 + 0), g = getv(i * 3 + 1), b = getv(i * 3 + 2);
            // Tonal (per-channel): contrast + shadows lift + highlight rolloff.
            auto tone = [&](float v) {
                if (contrast != 0.0f) v = (v - 0.5f) * cFactor + 0.5f;
                if (shadows != 0.0f) { float wgt = (1.0f - v) * (1.0f - v); v += shadows * wgt * 0.5f; }
                if (highlights != 0.0f) { float wgt = v * v; v += highlights * wgt * 0.5f; }
                return v;
            };
            r = tone(r); g = tone(g); b = tone(b);
            // Colour: saturation + vibrance around luma.
            if (sat != 0.0f || vib != 0.0f) {
                float luma = 0.299f * r + 0.587f * g + 0.114f * b;
                float mx = r > g ? (r > b ? r : b) : (g > b ? g : b);
                float mn = r < g ? (r < b ? r : b) : (g < b ? g : b);
                float chroma = mx - mn;                 // 0 (grey) .. 1 (vivid)
                float amt = sat + vib * (1.0f - chroma); // vibrance spares saturated px
                float s = 1.0f + amt;
                r = luma + (r - luma) * s;
                g = luma + (g - luma) * s;
                b = luma + (b - luma) * s;
            }
            setv(i * 3 + 0, r); setv(i * 3 + 1, g); setv(i * 3 + 2, b);
        }
    }

    // Unsharp mask: 3x3 box blur then add weighted difference. The box blur is
    // separable, so we stream it row-by-row keeping only three horizontally-blurred
    // rows (y-1, y, y+1) instead of a full-frame w*h*3 float buffer — bounded memory
    // even for full-res exports. Result is identical to the 2D count-based average.
    if (sharpen > 0.0f && w >= 3 && h >= 3) {
        const size_t rowLen = static_cast<size_t>(w) * 3;
        float* H[3] = {
            static_cast<float*>(malloc(rowLen * sizeof(float))),
            static_cast<float*>(malloc(rowLen * sizeof(float))),
            static_cast<float*>(malloc(rowLen * sizeof(float))),
        };
        if (H[0] != nullptr && H[1] != nullptr && H[2] != nullptr) {
            // Horizontal 1D box blur (3 wide, edge-clamped by count) of source row r.
            auto computeH = [&](int r, float* dst) {
                const size_t base = static_cast<size_t>(r) * w * 3;
                for (int x = 0; x < w; ++x) {
                    const int x0 = x > 0 ? x - 1 : 0;
                    const int x1 = x < w - 1 ? x + 1 : w - 1;
                    const float cnt = static_cast<float>(x1 - x0 + 1);
                    for (int c = 0; c < 3; ++c) {
                        float acc = 0.0f;
                        for (int xx = x0; xx <= x1; ++xx) acc += getv(base + static_cast<size_t>(xx) * 3 + c);
                        dst[static_cast<size_t>(x) * 3 + c] = acc / cnt;
                    }
                }
            };
            auto Hrow = [&](int r) -> float* { return H[r % 3]; };
            computeH(0, Hrow(0));
            for (int y = 0; y < h; ++y) {
                if (y + 1 < h) computeH(y + 1, Hrow(y + 1)); // read ahead before row y is overwritten
                const int y0 = y > 0 ? y - 1 : 0;
                const int y1 = y < h - 1 ? y + 1 : h - 1;
                const float vcnt = static_cast<float>(y1 - y0 + 1);
                const size_t base = static_cast<size_t>(y) * w * 3;
                for (size_t k = 0; k < rowLen; ++k) {
                    float blur = 0.0f;
                    for (int yy = y0; yy <= y1; ++yy) blur += Hrow(yy)[k];
                    blur /= vcnt;
                    const float v = getv(base + k);
                    setv(base + k, v + sharpen * (v - blur));
                }
            }
        }
        free(H[0]); free(H[1]); free(H[2]);
    }
}

// Export/decode path: tone over the LibRaw interleaved RGB buffer (8- or 16-bit).
void applyTone(libraw_processed_image_t* img, const DevelopParams& d) {
    if (img == nullptr || img->type != LIBRAW_IMAGE_BITMAP || img->colors != 3) return;
    const int w = img->width, h = img->height;
    const bool is16 = img->bits == 16;
    const float maxV = is16 ? 65535.0f : 255.0f;
    auto* d8 = img->data;
    auto* d16 = reinterpret_cast<uint16_t*>(img->data);
    auto getv = [&](size_t idx) -> float { return (is16 ? d16[idx] : d8[idx]) / maxV; };
    auto setv = [&](size_t idx, float v) {
        v = v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
        if (is16) d16[idx] = static_cast<uint16_t>(v * maxV + 0.5f);
        else d8[idx] = static_cast<uint8_t>(v * maxV + 0.5f);
    };
    applyToneCore(w, h, d, getv, setv);
}

// Live-preview path: tone over a packed 8-bit ARGB (0xAARRGGBB) buffer in place, so
// the editor re-applies tone without a re-demosaic while matching export exactly.
static void applyToneArgb(uint32_t* argb, int w, int h, const DevelopParams& d) {
    auto chShift = [](size_t idx) -> int { const int c = static_cast<int>(idx % 3); return c == 0 ? 16 : (c == 1 ? 8 : 0); };
    auto getv = [&](size_t idx) -> float {
        return ((argb[idx / 3] >> chShift(idx)) & 0xFFu) / 255.0f;
    };
    auto setv = [&](size_t idx, float v) {
        v = v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
        const int shift = chShift(idx);
        const uint32_t iv = static_cast<uint32_t>(v * 255.0f + 0.5f) & 0xFFu;
        argb[idx / 3] = (argb[idx / 3] & ~(0xFFu << shift)) | (iv << shift);
    };
    applyToneCore(w, h, d, getv, setv);
}

// Runs open/unpack/process and returns the processed image (caller frees), or nullptr.
libraw_processed_image_t* process(LibRaw& raw, const uint8_t* data, size_t size) {
    if (raw.open_buffer(const_cast<uint8_t*>(data), size) != LIBRAW_SUCCESS) return nullptr;
    if (raw.unpack() != LIBRAW_SUCCESS) return nullptr;
    if (raw.dcraw_process() != LIBRAW_SUCCESS) return nullptr;
    int err = 0;
    libraw_processed_image_t* img = raw.dcraw_make_mem_image(&err);
    if (err != 0) {
        if (img != nullptr) LibRaw::dcraw_clear_mem(img);
        return nullptr;
    }
    return img;
}

// Packs an 8-bit interleaved RGB image into a Java int[] laid out [w, h, ARGB...].
jintArray toArgbIntArray(JNIEnv* env, const uint8_t* rgb, int w, int h) {
    if (rgb == nullptr || w <= 0 || h <= 0) return nullptr;
    const jsize total = static_cast<jsize>(w) * static_cast<jsize>(h) + 2;
    jintArray out = env->NewIntArray(total);
    if (out == nullptr) return nullptr;
    jint* buf = env->GetIntArrayElements(out, nullptr);
    if (buf == nullptr) return nullptr;
    buf[0] = w;
    buf[1] = h;
    jint* dst = buf + 2;
    const size_t px = static_cast<size_t>(w) * static_cast<size_t>(h);
    for (size_t i = 0; i < px; ++i) {
        const uint8_t r = rgb[i * 3 + 0];
        const uint8_t g = rgb[i * 3 + 1];
        const uint8_t b = rgb[i * 3 + 2];
        dst[i] = (static_cast<jint>(0xFF) << 24) |
                 (static_cast<jint>(r) << 16) |
                 (static_cast<jint>(g) << 8) |
                 (static_cast<jint>(b));
    }
    env->ReleaseIntArrayElements(out, buf, 0);
    return out;
}

} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_core_decoder_NativeRawDecoder_nativeSelfTest(JNIEnv*, jclass) {
    return JNI_TRUE;
}

// Returns [fullW, fullH, thumbW, thumbH, isFoveon, flip] or null.
extern "C" JNIEXPORT jintArray JNICALL
Java_com_dot_gallery_core_decoder_NativeRawDecoder_nativeGetInfo(
        JNIEnv* env, jclass, jbyteArray dataArr) {
    if (dataArr == nullptr) return nullptr;
    const jsize size = env->GetArrayLength(dataArr);
    if (size <= 0) return nullptr;
    auto* data = static_cast<uint8_t*>(malloc(size));
    if (data == nullptr) return nullptr;
    env->GetByteArrayRegion(dataArr, 0, size, reinterpret_cast<jbyte*>(data));

    LibRaw raw;
    jintArray out = nullptr;
    if (raw.open_buffer(data, size) == LIBRAW_SUCCESS) {
        const auto& s = raw.imgdata.sizes;
        const auto& t = raw.imgdata.thumbnail;
        jint info[6] = {
            static_cast<jint>(s.width), static_cast<jint>(s.height),
            static_cast<jint>(t.twidth), static_cast<jint>(t.theight),
            static_cast<jint>(raw.imgdata.idata.is_foveon), static_cast<jint>(s.flip)
        };
        out = env->NewIntArray(6);
        if (out != nullptr) env->SetIntArrayRegion(out, 0, 6, info);
    }
    raw.recycle();
    free(data);
    return out;
}

// Decodes the embedded camera thumbnail. Returns [w, h, ARGB...] or null.
extern "C" JNIEXPORT jintArray JNICALL
Java_com_dot_gallery_core_decoder_NativeRawDecoder_nativeGetThumbnail(
        JNIEnv* env, jclass, jbyteArray dataArr) {
    if (dataArr == nullptr) return nullptr;
    const jsize size = env->GetArrayLength(dataArr);
    if (size <= 0) return nullptr;
    auto* data = static_cast<uint8_t*>(malloc(size));
    if (data == nullptr) return nullptr;
    env->GetByteArrayRegion(dataArr, 0, size, reinterpret_cast<jbyte*>(data));

    LibRaw raw;
    jintArray out = nullptr;
    if (raw.open_buffer(data, size) == LIBRAW_SUCCESS &&
        raw.unpack_thumb() == LIBRAW_SUCCESS) {
        int err = 0;
        libraw_processed_image_t* thumb = raw.dcraw_make_mem_thumb(&err);
        if (thumb != nullptr && err == 0) {
            if (thumb->type == LIBRAW_IMAGE_BITMAP && thumb->colors == 3 && thumb->bits == 8) {
                out = toArgbIntArray(env, thumb->data, thumb->width, thumb->height);
            }
            // JPEG-typed thumbnails are decoded on the Kotlin side (BitmapFactory) instead.
        }
        if (thumb != nullptr) LibRaw::dcraw_clear_mem(thumb);
    }
    raw.recycle();
    free(data);
    return out;
}

// Demosaics to 8-bit and returns [w, h, ARGB...] or null.
extern "C" JNIEXPORT jintArray JNICALL
Java_com_dot_gallery_core_decoder_NativeRawDecoder_nativeDemosaic(
        JNIEnv* env, jclass, jbyteArray dataArr,
        jintArray intParams, jfloatArray floatParams, jfloatArray userMul) {
    if (dataArr == nullptr) return nullptr;
    const jsize size = env->GetArrayLength(dataArr);
    if (size <= 0) return nullptr;
    auto* data = static_cast<uint8_t*>(malloc(size));
    if (data == nullptr) return nullptr;
    env->GetByteArrayRegion(dataArr, 0, size, reinterpret_cast<jbyte*>(data));

    LibRaw raw;
    DevelopParams d = readParams(env, intParams, floatParams, userMul);
    applyParams(raw, d, 8);
    jintArray out = nullptr;
    libraw_processed_image_t* img = process(raw, data, static_cast<size_t>(size));
    if (img != nullptr) {
        if (img->type == LIBRAW_IMAGE_BITMAP && img->colors == 3 && img->bits == 8) {
            applyTone(img, d);
            out = toArgbIntArray(env, img->data, img->width, img->height);
        } else {
            LOGE("unexpected demosaic output: type=%d colors=%d bits=%d",
                 img->type, img->colors, img->bits);
        }
        LibRaw::dcraw_clear_mem(img);
    }
    raw.recycle();
    free(data);
    return out;
}

// Demosaics at [outputBps] (8 or 16) and writes a TIFF to [fd] via libtiff. Returns success.
extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_core_decoder_NativeRawDecoder_nativeExportTiff(
        JNIEnv* env, jclass, jbyteArray dataArr,
        jintArray intParams, jfloatArray floatParams, jfloatArray userMul,
        jint outputBps, jint fd, jint compression) {
    if (dataArr == nullptr || fd < 0) return JNI_FALSE;
    const jsize size = env->GetArrayLength(dataArr);
    if (size <= 0) return JNI_FALSE;
    const int bps = (outputBps == 16) ? 16 : 8;
    auto* data = static_cast<uint8_t*>(malloc(size));
    if (data == nullptr) return JNI_FALSE;
    env->GetByteArrayRegion(dataArr, 0, size, reinterpret_cast<jbyte*>(data));

    LibRaw raw;
    DevelopParams d = readParams(env, intParams, floatParams, userMul);
    applyParams(raw, d, bps);
    libraw_processed_image_t* img = process(raw, data, static_cast<size_t>(size));
    jboolean result = JNI_FALSE;
    if (img != nullptr && img->type == LIBRAW_IMAGE_BITMAP && img->colors == 3) {
        applyTone(img, d);
        const int w = img->width;
        const int h = img->height;
        const int dupFd = dup(fd);   // TIFFClose closes its fd; keep the caller's PFD valid.
        TIFF* tif = (dupFd >= 0) ? TIFFFdOpen(dupFd, "raw_export.tif", "w") : nullptr;
        if (tif != nullptr) {
            TIFFSetField(tif, TIFFTAG_IMAGEWIDTH, w);
            TIFFSetField(tif, TIFFTAG_IMAGELENGTH, h);
            TIFFSetField(tif, TIFFTAG_SAMPLESPERPIXEL, 3);
            TIFFSetField(tif, TIFFTAG_BITSPERSAMPLE, img->bits);
            TIFFSetField(tif, TIFFTAG_ORIENTATION, ORIENTATION_TOPLEFT);
            TIFFSetField(tif, TIFFTAG_PLANARCONFIG, PLANARCONFIG_CONTIG);
            TIFFSetField(tif, TIFFTAG_PHOTOMETRIC, PHOTOMETRIC_RGB);
            // compression: 0=none(1), 1=deflate(8), 2=lzw(5)
            uint16_t comp = COMPRESSION_ADOBE_DEFLATE;
            if (compression == 0) comp = COMPRESSION_NONE;
            else if (compression == 2) comp = COMPRESSION_LZW;
            TIFFSetField(tif, TIFFTAG_COMPRESSION, comp);
            TIFFSetField(tif, TIFFTAG_ROWSPERSTRIP, TIFFDefaultStripSize(tif, 0));

            const size_t bytesPerSample = img->bits / 8;
            const tmsize_t rowBytes = static_cast<tmsize_t>(w) * 3 * bytesPerSample;
            bool ok = true;
            for (int y = 0; y < h && ok; ++y) {
                void* row = img->data + static_cast<size_t>(y) * rowBytes;
                if (TIFFWriteScanline(tif, row, y, 0) < 0) ok = false;
            }
            TIFFClose(tif);
            result = ok ? JNI_TRUE : JNI_FALSE;
        } else if (dupFd >= 0) {
            close(dupFd);
        }
    }
    if (img != nullptr) LibRaw::dcraw_clear_mem(img);
    raw.recycle();
    free(data);
    return result;
}

// Fast tone re-apply on a packed ARGB buffer (no re-demosaic). Reads the tone
// float bundle, applies it to a copy of [argbArr], and returns [w, h, ARGB...] to
// match packedToBitmap. The input array is left unchanged.
extern "C" JNIEXPORT jintArray JNICALL
Java_com_dot_gallery_core_decoder_NativeRawDecoder_nativeApplyTone(
        JNIEnv* env, jclass, jintArray argbArr, jint w, jint h, jfloatArray floatParams) {
    if (argbArr == nullptr || w <= 0 || h <= 0) return nullptr;
    const size_t px = static_cast<size_t>(w) * static_cast<size_t>(h);
    const jsize len = env->GetArrayLength(argbArr);
    if (static_cast<size_t>(len) < px) return nullptr;

    DevelopParams d{};
    for (int i = 0; i < I_COUNT; ++i) d.ip[i] = 0;
    for (int i = 0; i < F_COUNT; ++i) d.fp[i] = 0.0f;
    if (floatParams != nullptr) {
        const jsize n = env->GetArrayLength(floatParams);
        env->GetFloatArrayRegion(floatParams, 0, n < F_COUNT ? n : F_COUNT, d.fp);
    }

    jint* src = env->GetIntArrayElements(argbArr, nullptr);
    if (src == nullptr) return nullptr;
    applyToneArgb(reinterpret_cast<uint32_t*>(src), w, h, d);

    jintArray out = env->NewIntArray(static_cast<jsize>(2 + px));
    if (out != nullptr) {
        const jint hdr[2] = { w, h };
        env->SetIntArrayRegion(out, 0, 2, hdr);
        env->SetIntArrayRegion(out, 2, static_cast<jsize>(px), src);
    }
    // JNI_ABORT: discard our in-place edits to the caller's array (output is separate).
    env->ReleaseIntArrayElements(argbArr, src, JNI_ABORT);
    return out;
}

#else // !HAVE_LIBRAW — stubs so the .so still loads and Kotlin falls back to embedded preview.

extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_core_decoder_NativeRawDecoder_nativeSelfTest(JNIEnv*, jclass) {
    return JNI_FALSE;
}
extern "C" JNIEXPORT jintArray JNICALL
Java_com_dot_gallery_core_decoder_NativeRawDecoder_nativeGetInfo(JNIEnv*, jclass, jbyteArray) {
    return nullptr;
}
extern "C" JNIEXPORT jintArray JNICALL
Java_com_dot_gallery_core_decoder_NativeRawDecoder_nativeGetThumbnail(JNIEnv*, jclass, jbyteArray) {
    return nullptr;
}
extern "C" JNIEXPORT jintArray JNICALL
Java_com_dot_gallery_core_decoder_NativeRawDecoder_nativeDemosaic(
        JNIEnv*, jclass, jbyteArray, jintArray, jfloatArray, jfloatArray) {
    return nullptr;
}
extern "C" JNIEXPORT jboolean JNICALL
Java_com_dot_gallery_core_decoder_NativeRawDecoder_nativeExportTiff(
        JNIEnv*, jclass, jbyteArray, jintArray, jfloatArray, jfloatArray,
        jint, jint, jint) {
    return JNI_FALSE;
}
extern "C" JNIEXPORT jintArray JNICALL
Java_com_dot_gallery_core_decoder_NativeRawDecoder_nativeApplyTone(
        JNIEnv*, jclass, jintArray, jint, jint, jfloatArray) {
    return nullptr;
}

#endif // HAVE_LIBRAW
