/*
 * MIT License
 *
 * Copyright (c) 2023 Radzivon Bartoshyk
 * jxl-coder [https://github.com/awxkee/jxl-coder]
 *
 * Created by Radzivon Bartoshyk on 18/9/2023
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

package com.dot.gallery.feature_node.presentation.util

import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.Scale
import coil3.size.Size
import coil3.size.pxOrElse
import com.awxkee.jxlcoder.JxlCoder
import com.awxkee.jxlcoder.JxlResizeFilter
import com.awxkee.jxlcoder.PreferredColorConfig
import com.awxkee.jxlcoder.ScaleMode
import kotlinx.coroutines.runInterruptible
import okio.BufferedSource
import okio.ByteString.Companion.toByteString

class JxlDecoder(
    private val source: SourceFetchResult,
    private val options: Options,
    private val imageLoader: ImageLoader
) : Decoder {

    @OptIn(ExperimentalCoilApi::class)
    override suspend fun decode(): DecodeResult = runInterruptible {
        // ColorSpace is preferred to be ignored due to lib is trying to handle all color profile by itself
        val sourceData = source.source.source().readByteArray()
        if (options.size == Size.ORIGINAL) {
            val originalImage =
                JxlCoder.decode(
                    sourceData,
                    preferredColorConfig = PreferredColorConfig.DEFAULT
                )
            return@runInterruptible DecodeResult(
                image = originalImage.asImage(),
                isSampled = false
            )
        }

        val dstWidth = options.size.width.pxOrElse { 0 }
        val dstHeight = options.size.height.pxOrElse { 0 }
        val scaleMode = when (options.scale) {
            Scale.FILL -> ScaleMode.FILL
            Scale.FIT -> ScaleMode.FIT
        }

        val originalImage =
            JxlCoder.decodeSampled(
                sourceData,
                dstWidth,
                dstHeight,
                preferredColorConfig = PreferredColorConfig.DEFAULT,
                scaleMode,
                JxlResizeFilter.BILINEAR,
            )
        DecodeResult(
            image = originalImage.asImage(),
            isSampled = true
        )
    }

    class Factory : Decoder.Factory {

        private val MAGIC_1 = byteArrayOf(0xFF.toByte(), 0x0A).toByteString()
        private val MAGIC_2 = byteArrayOf(
            0x0.toByte(),
            0x0.toByte(),
            0x0.toByte(),
            0x0C.toByte(),
            0x4A,
            0x58,
            0x4C,
            0x20,
            0x0D,
            0x0A,
            0x87.toByte(),
            0x0A
        ).toByteString()

        private fun isJXL(source: BufferedSource): Boolean {
            return source.rangeEquals(0, MAGIC_1) || source.rangeEquals(
                0,
                MAGIC_2
            )
        }

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader
        ) = if (isJXL(result.source.source())) {
            JxlDecoder(result, options, imageLoader)
        } else null
    }

}