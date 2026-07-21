/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.decoder

import com.dot.gallery.core.decoder.RawDemosaic
import com.dot.gallery.core.decoder.RawDevelopParams
import com.dot.gallery.core.decoder.RawHighlightMode
import com.dot.gallery.core.decoder.RawNoiseReduction
import com.dot.gallery.core.decoder.RawOutputColorSpace
import com.dot.gallery.core.decoder.RawWhiteBalance
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM tests for the RAW develop recipe -> native-int mapping and value semantics. */
class RawDevelopParamsTest {

    @Test
    fun `native enum values match LibRaw contract`() {
        assertEquals(0, RawOutputColorSpace.RAW.nativeValue)
        assertEquals(1, RawOutputColorSpace.SRGB.nativeValue)
        assertEquals(2, RawOutputColorSpace.ADOBE_RGB.nativeValue)
        assertEquals(3, RawOutputColorSpace.WIDE_GAMUT.nativeValue)
        assertEquals(4, RawOutputColorSpace.PROPHOTO.nativeValue)

        assertEquals(0, RawDemosaic.LINEAR.nativeValue)
        assertEquals(1, RawDemosaic.VNG.nativeValue)
        assertEquals(2, RawDemosaic.PPG.nativeValue)
        assertEquals(3, RawDemosaic.AHD.nativeValue)
        assertEquals(4, RawDemosaic.DCB.nativeValue)

        assertEquals(0, RawHighlightMode.CLIP.nativeValue)
        assertEquals(1, RawHighlightMode.UNCLIP.nativeValue)
        assertEquals(2, RawHighlightMode.BLEND.nativeValue)
        assertEquals(5, RawHighlightMode.REBUILD.nativeValue)
    }

    @Test
    fun `AUTO default is neutral camera-WB sRGB AHD 8-bit`() {
        val p = RawDevelopParams.AUTO
        assertTrue(p.useCameraWb)
        assertFalse(p.useAutoWb)
        assertNull(p.effectiveUserMul)
        assertEquals(RawOutputColorSpace.SRGB, p.outputColorSpace)
        assertEquals(RawDemosaic.AHD, p.demosaic)
        assertEquals(8, p.outputBits)
        assertEquals(0f, p.exposureEv)
        assertFalse(p.halfSize)
    }

    @Test
    fun `white balance flags resolve per source`() {
        assertTrue(RawDevelopParams(whiteBalance = RawWhiteBalance.CAMERA).useCameraWb)
        assertTrue(RawDevelopParams(whiteBalance = RawWhiteBalance.AUTO).useAutoWb)
        // Daylight: neither camera nor auto (LibRaw uses embedded daylight multipliers).
        val daylight = RawDevelopParams(whiteBalance = RawWhiteBalance.DAYLIGHT)
        assertFalse(daylight.useCameraWb)
        assertFalse(daylight.useAutoWb)
        assertNull(daylight.effectiveUserMul)
    }

    @Test
    fun `custom white balance exposes user multipliers only when CUSTOM`() {
        val mul = floatArrayOf(2.1f, 1f, 1.5f, 1f)
        val custom = RawDevelopParams(whiteBalance = RawWhiteBalance.CUSTOM, userMul = mul)
        assertArrayEquals(mul, custom.effectiveUserMul!!, 0f)
        // userMul is ignored unless the source is CUSTOM.
        val camera = RawDevelopParams(whiteBalance = RawWhiteBalance.CAMERA, userMul = mul)
        assertNull(camera.effectiveUserMul)
    }

    @Test
    fun `toIntParams matches the JNI layout`() {
        val p = RawDevelopParams(
            whiteBalance = RawWhiteBalance.AUTO,
            highlight = RawHighlightMode.BLEND,
            outputColorSpace = RawOutputColorSpace.ADOBE_RGB,
            demosaic = RawDemosaic.DCB,
            halfSize = true,
            noiseReduction = RawNoiseReduction.MEDIUM,
            outputBits = 16,
        )
        val ip = p.toIntParams(userFlip = 6)
        assertEquals(0, ip[0]) // useCameraWb
        assertEquals(1, ip[1]) // useAutoWb
        assertEquals(RawHighlightMode.BLEND.nativeValue, ip[2])
        assertEquals(RawOutputColorSpace.ADOBE_RGB.nativeValue, ip[3])
        assertEquals(RawDemosaic.DCB.nativeValue, ip[4])
        assertEquals(1, ip[5]) // halfSize
        assertEquals(6, ip[6]) // userFlip
        assertEquals(RawNoiseReduction.MEDIUM.fbdd, ip[7])
        assertEquals(16, ip[8]) // outputBits
    }

    @Test
    fun `toFloatParams matches the JNI layout`() {
        val p = RawDevelopParams(
            exposureEv = 1f,
            brightness = 0.5f,
            contrast = 0.2f,
            saturation = 0.3f,
            vibrance = 0.4f,
            shadows = -0.1f,
            highlightsTone = -0.2f,
            sharpen = 1.5f,
            noiseReduction = RawNoiseReduction.HIGH,
        )
        val fp = p.toFloatParams()
        assertEquals(1f, fp[0]) // exposure
        assertEquals(0.5f, fp[1]) // brightness
        assertEquals(0.2f, fp[2]) // contrast
        assertEquals(0.3f, fp[3]) // saturation
        assertEquals(0.4f, fp[4]) // vibrance
        assertEquals(-0.1f, fp[5]) // shadows
        assertEquals(-0.2f, fp[6]) // highlights
        assertEquals(1.5f, fp[7]) // sharpen
        assertEquals(RawNoiseReduction.HIGH.threshold, fp[8]) // NR threshold
    }

    @Test
    fun `custom temp tint derives multipliers`() {
        val warm = RawDevelopParams(whiteBalance = RawWhiteBalance.CUSTOM, wbTempKelvin = 3000)
        val mul = warm.effectiveUserMul!!
        // Warmer than the 6500K anchor boosts red over blue.
        assertTrue(mul[0] > mul[2])
    }

    @Test
    fun `equals and hashCode account for userMul contents`() {
        val a = RawDevelopParams(whiteBalance = RawWhiteBalance.CUSTOM, userMul = floatArrayOf(1f, 2f, 3f, 4f))
        val b = RawDevelopParams(whiteBalance = RawWhiteBalance.CUSTOM, userMul = floatArrayOf(1f, 2f, 3f, 4f))
        val c = RawDevelopParams(whiteBalance = RawWhiteBalance.CUSTOM, userMul = floatArrayOf(9f, 2f, 3f, 4f))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertFalse(a == c)
    }
}
