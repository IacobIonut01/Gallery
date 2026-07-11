/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder.glide

import android.graphics.Bitmap
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.dot.gallery.core.decoder.format.CameraRawImageDecoder
import com.dot.gallery.core.decoder.format.CameraRawMime

/**
 * Camera RAW decoder for the MimeInputStream (content Uri) path. Extracts the embedded JPEG
 * preview/thumbnail from TIFF-based RAW containers.
 */
class CameraRawMimeInputStreamDecoder(
    private val bitmapPool: BitmapPool
) : ResourceDecoder<MimeInputStream, Bitmap> {

    override fun handles(source: MimeInputStream, options: Options): Boolean =
        CameraRawMime.isCameraRaw(source.mimeType)

    override fun decode(
        source: MimeInputStream,
        width: Int,
        height: Int,
        options: Options
    ): Resource<Bitmap>? {
        val data = source.inputStream.readBytes()
        val bmp = CameraRawImageDecoder.decode(data, width, height, source.mimeType) ?: return null
        return BitmapResource.obtain(bmp, bitmapPool)
    }
}
