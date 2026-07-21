/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder

import kotlin.math.pow

/** White-balance source for RAW develop. */
enum class RawWhiteBalance { CAMERA, AUTO, DAYLIGHT, CUSTOM }

/** Noise-reduction strength, mapped to LibRaw's FBDD + wavelet threshold. */
enum class RawNoiseReduction(val fbdd: Int, val threshold: Float) {
    OFF(0, 0f), LOW(1, 100f), MEDIUM(2, 300f), HIGH(2, 700f)
}

/** Output colour space, mapped to LibRaw's `output_color`. */
enum class RawOutputColorSpace(val nativeValue: Int) {
    RAW(0), SRGB(1), ADOBE_RGB(2), WIDE_GAMUT(3), PROPHOTO(4)
}

/** Demosaic (interpolation) algorithm, mapped to LibRaw's `user_qual`. */
enum class RawDemosaic(val nativeValue: Int) {
    LINEAR(0), VNG(1), PPG(2), AHD(3), DCB(4)
}

/** Highlight-recovery mode, mapped to LibRaw's `highlight`. */
enum class RawHighlightMode(val nativeValue: Int) {
    CLIP(0), UNCLIP(1), BLEND(2), REBUILD(5)
}

/** TIFF output compression for export, mapped to the JNI `compression` arg. */
enum class RawTiffCompression(val nativeValue: Int) { NONE(0), DEFLATE(1), LZW(2) }

/**
 * Resolution-independent RAW develop recipe passed to [NativeRawDecoder]. Defaults reproduce a
 * neutral, automatic develop (camera white balance, sRGB, AHD demosaic, 8-bit) so callers that
 * don't tune anything still get a faithful render.
 */
data class RawDevelopParams(
    val whiteBalance: RawWhiteBalance = RawWhiteBalance.CAMERA,
    /** Custom WB multipliers [R, G, B, G2]; only used when [whiteBalance] == CUSTOM. */
    val userMul: FloatArray? = null,
    /** WB temperature in Kelvin; when set (CUSTOM), converted to [userMul]. */
    val wbTempKelvin: Int? = null,
    /** WB green–magenta tint in [-1, 1]; positive adds green. */
    val wbTint: Float = 0f,
    val exposureEv: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val vibrance: Float = 0f,
    val shadows: Float = 0f,
    val highlightsTone: Float = 0f,
    val sharpen: Float = 0f,
    val noiseReduction: RawNoiseReduction = RawNoiseReduction.OFF,
    val highlight: RawHighlightMode = RawHighlightMode.CLIP,
    val outputColorSpace: RawOutputColorSpace = RawOutputColorSpace.SRGB,
    val demosaic: RawDemosaic = RawDemosaic.AHD,
    val halfSize: Boolean = false,
    val outputBits: Int = 8,
) {
    val useCameraWb: Boolean get() = whiteBalance == RawWhiteBalance.CAMERA
    val useAutoWb: Boolean get() = whiteBalance == RawWhiteBalance.AUTO

    /**
     * The same recipe with every post-demosaic tone field reset to neutral. Demosaicing with this
     * yields a "base" bitmap the editor can re-tone (via [NativeRawDecoder.applyTone]) without
     * re-decoding. Exposure/brightness stay because LibRaw bakes them during demosaic (base stage).
     */
    val baseOnly: RawDevelopParams
        get() = copy(
            contrast = 0f, saturation = 0f, vibrance = 0f,
            shadows = 0f, highlightsTone = 0f, sharpen = 0f,
        )

    /** True when any post-demosaic tone op is non-neutral. */
    val hasTone: Boolean
        get() = contrast != 0f || saturation != 0f || vibrance != 0f ||
            shadows != 0f || highlightsTone != 0f || sharpen != 0f

    /**
     * True when [other] differs only in post-demosaic tone fields (the base demosaic — white
     * balance, exposure, demosaic algo, colour space, highlight, noise reduction, half-size — is
     * unchanged), so the editor can re-tone the cached base instead of re-demosaicing.
     */
    fun sharesBaseWith(other: RawDevelopParams): Boolean = baseOnly == other.baseOnly

    /**
     * CUSTOM uses [userMul] (explicit) or a temp/tint-derived multiplier; DAYLIGHT falls through to
     * LibRaw's embedded daylight multipliers; CAMERA/AUTO use no explicit multiplier.
     */
    val effectiveUserMul: FloatArray?
        get() = when (whiteBalance) {
            RawWhiteBalance.CUSTOM -> userMul ?: wbTempKelvin?.let { tempTintToMul(it, wbTint) }
            else -> null
        }

    /** int[] bundle matching the JNI layout; [userFlip] < 0 keeps LibRaw's parsed orientation. */
    fun toIntParams(userFlip: Int = -1): IntArray = intArrayOf(
        if (useCameraWb) 1 else 0,
        if (useAutoWb) 1 else 0,
        highlight.nativeValue,
        outputColorSpace.nativeValue,
        demosaic.nativeValue,
        if (halfSize) 1 else 0,
        userFlip,
        noiseReduction.fbdd,
        outputBits,
    )

    /** float[] bundle matching the JNI layout. */
    fun toFloatParams(): FloatArray = floatArrayOf(
        exposureEv,
        brightness,
        contrast,
        saturation,
        vibrance,
        shadows,
        highlightsTone,
        sharpen,
        noiseReduction.threshold,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RawDevelopParams) return false
        return whiteBalance == other.whiteBalance &&
            (userMul?.contentEquals(other.userMul) ?: (other.userMul == null)) &&
            wbTempKelvin == other.wbTempKelvin &&
            wbTint == other.wbTint &&
            exposureEv == other.exposureEv &&
            brightness == other.brightness &&
            contrast == other.contrast &&
            saturation == other.saturation &&
            vibrance == other.vibrance &&
            shadows == other.shadows &&
            highlightsTone == other.highlightsTone &&
            sharpen == other.sharpen &&
            noiseReduction == other.noiseReduction &&
            highlight == other.highlight &&
            outputColorSpace == other.outputColorSpace &&
            demosaic == other.demosaic &&
            halfSize == other.halfSize &&
            outputBits == other.outputBits
    }

    override fun hashCode(): Int {
        var result = whiteBalance.hashCode()
        result = 31 * result + (userMul?.contentHashCode() ?: 0)
        result = 31 * result + (wbTempKelvin ?: 0)
        result = 31 * result + wbTint.hashCode()
        result = 31 * result + exposureEv.hashCode()
        result = 31 * result + brightness.hashCode()
        result = 31 * result + contrast.hashCode()
        result = 31 * result + saturation.hashCode()
        result = 31 * result + vibrance.hashCode()
        result = 31 * result + shadows.hashCode()
        result = 31 * result + highlightsTone.hashCode()
        result = 31 * result + sharpen.hashCode()
        result = 31 * result + noiseReduction.hashCode()
        result = 31 * result + highlight.hashCode()
        result = 31 * result + outputColorSpace.hashCode()
        result = 31 * result + demosaic.hashCode()
        result = 31 * result + halfSize.hashCode()
        result = 31 * result + outputBits
        return result
    }

    companion object {
        /** Neutral automatic develop. */
        val AUTO = RawDevelopParams()

        /**
         * Approximate camera-neutral WB multipliers for a target [kelvin] temperature and [tint].
         * Uses a simple black-body approximation (relative R/B against a 6500 K daylight anchor);
         * good enough for interactive WB nudging, not colour-managed accuracy.
         */
        fun tempTintToMul(kelvin: Int, tint: Float): FloatArray {
            val k = kelvin.coerceIn(2000, 12000).toFloat()
            // Warmer (lower K) → boost red, cut blue; cooler (higher K) → the reverse.
            val ratio = 6500f / k
            val r = ratio.pow(1.2f)
            val b = (1f / ratio).pow(1.2f)
            val g = 1f + tint.coerceIn(-1f, 1f) * 0.3f
            return floatArrayOf(r, g, b, g)
        }
    }
}
