/*
 * MIT License
 *
 * Copyright (c) 2023 Radzivon Bartoshyk
 * avif-coder [https://github.com/awxkee/avif-coder]
 *
 * Created by Radzivon Bartoshyk on 23/09/2023
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
// Same HeifDecoder but for coil3

package com.github.awxkee.avifcoil.decoder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.allowRgb565
import coil3.request.bitmapConfig
import coil3.size.Scale
import coil3.size.Size
import coil3.size.pxOrElse
import com.radzivon.bartoshyk.avif.coder.HeifCoder
import com.radzivon.bartoshyk.avif.coder.PreferredColorConfig
import com.radzivon.bartoshyk.avif.coder.ScaleMode
import kotlinx.coroutines.runInterruptible
import okio.ByteString.Companion.encodeUtf8

class HeifDecoder3(
    context: Context?,
    private val source: SourceFetchResult,
    private val options: Options,
) : Decoder {

    private val coder = HeifCoder(context)

    @OptIn(ExperimentalCoilApi::class)
    override suspend fun decode(): DecodeResult = runInterruptible {
        // ColorSpace is preferred to be ignored due to lib is trying to handle all color profile by itself
        val sourceData = source.source.source().readByteArray()

        var mPreferredColorConfig: PreferredColorConfig = when (options.bitmapConfig) {
            Bitmap.Config.ALPHA_8 -> PreferredColorConfig.RGBA_8888
            Bitmap.Config.RGB_565 -> if (options.allowRgb565) PreferredColorConfig.RGB_565 else PreferredColorConfig.DEFAULT
            Bitmap.Config.ARGB_8888 -> PreferredColorConfig.RGBA_8888
            else -> PreferredColorConfig.DEFAULT
        }
        if (options.bitmapConfig == Bitmap.Config.RGBA_F16) {
            mPreferredColorConfig = PreferredColorConfig.RGBA_F16
        } else if (options.bitmapConfig == Bitmap.Config.HARDWARE) {
            mPreferredColorConfig = PreferredColorConfig.HARDWARE
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && options.bitmapConfig == Bitmap.Config.RGBA_1010102) {
            mPreferredColorConfig = PreferredColorConfig.RGBA_1010102
        }

        if (options.size == Size.ORIGINAL) {
            val originalImage =
                coder.decode(
                    sourceData,
                    preferredColorConfig = mPreferredColorConfig
                )
            return@runInterruptible DecodeResult(
                BitmapDrawable(
                    options.context.resources,
                    originalImage
                ).asImage(), false
            )
        }

        val dstWidth = options.size.width.pxOrElse { 0 }
        val dstHeight = options.size.height.pxOrElse { 0 }
        val scaleMode = when (options.scale) {
            Scale.FILL -> ScaleMode.FILL
            Scale.FIT -> ScaleMode.FIT
        }

        val originalImage =
            coder.decodeSampled(
                sourceData,
                dstWidth,
                dstHeight,
                preferredColorConfig = mPreferredColorConfig,
                scaleMode,
            )
        return@runInterruptible DecodeResult(
            image = BitmapDrawable(
                options.context.resources,
                originalImage
            ).asImage(), isSampled = true
        )
    }

    /**
     * @param context is preferred to be set when displaying an HDR content to apply Vulkan shaders
     */
    class Factory(private val context: Context? = null) : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader
        ): Decoder? {
            return if (AVAILABLE_BRANDS.any {
                    result.source.source().rangeEquals(4, it)
                }) HeifDecoder3(context, result, options) else null
        }

        companion object {
            private val MIF = "ftypmif1".encodeUtf8()
            private val MSF = "ftypmsf1".encodeUtf8()
            private val HEIC = "ftypheic".encodeUtf8()
            private val HEIX = "ftypheix".encodeUtf8()
            private val HEVC = "ftyphevc".encodeUtf8()
            private val HEVX = "ftyphevx".encodeUtf8()
            private val AVIF = "ftypavif".encodeUtf8()
            private val AVIS = "ftypavis".encodeUtf8()

            private val AVAILABLE_BRANDS = listOf(MIF, MSF, HEIC, HEIX, HEVC, HEVX, AVIF, AVIS)
        }
    }
}
