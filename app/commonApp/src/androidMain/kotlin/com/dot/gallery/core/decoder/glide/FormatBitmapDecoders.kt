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
import com.dot.gallery.core.decoder.format.ImageFormatSniffer
import com.dot.gallery.core.decoder.format.Jp2ImageDecoder
import com.dot.gallery.core.decoder.format.PsdImageDecoder
import com.dot.gallery.core.decoder.format.SvgImageDecoder
import java.io.InputStream

/**
 * Glide [ResourceDecoder]s for formats Android cannot decode natively. They detect the format by
 * magic bytes (MIME from MediaStore/MimeTypeMap is unreliable for these) and delegate decoding to
 * the shared decoder cores. Registered on the InputStream -> Bitmap path so they work for both the
 * timeline grid and album covers.
 */

private fun InputStream.peek(count: Int): Pair<ByteArray, Int> {
    mark(count)
    val buf = ByteArray(count)
    val read = read(buf)
    reset()
    return buf to read
}

/** Adobe Photoshop (.psd / .psb). */
class PsdBitmapDecoder(
    private val bitmapPool: BitmapPool
) : ResourceDecoder<InputStream, Bitmap> {

    override fun handles(source: InputStream, options: Options): Boolean {
        val (head, read) = source.peek(8)
        return read >= 4 && ImageFormatSniffer.isPsd(head, read)
    }

    override fun decode(source: InputStream, width: Int, height: Int, options: Options): Resource<Bitmap>? {
        val bytes = source.readBytes()
        val bmp = PsdImageDecoder.decode(bytes, width, height) ?: return null
        return BitmapResource.obtain(bmp, bitmapPool)
    }
}

/** JPEG 2000 (.jp2 / .j2k). */
class Jp2BitmapDecoder(
    private val bitmapPool: BitmapPool
) : ResourceDecoder<InputStream, Bitmap> {

    override fun handles(source: InputStream, options: Options): Boolean {
        val (head, read) = source.peek(12)
        return read >= 4 && ImageFormatSniffer.isJp2(head, read)
    }

    override fun decode(source: InputStream, width: Int, height: Int, options: Options): Resource<Bitmap>? {
        val bytes = source.readBytes()
        val bmp = Jp2ImageDecoder.decode(bytes, width, height) ?: return null
        return BitmapResource.obtain(bmp, bitmapPool)
    }
}

/** SVG (rasterized via AndroidSVG). */
class SvgBitmapDecoder(
    private val bitmapPool: BitmapPool
) : ResourceDecoder<InputStream, Bitmap> {

    override fun handles(source: InputStream, options: Options): Boolean {
        val (head, read) = source.peek(1024)
        return read >= 4 && ImageFormatSniffer.isSvg(head, read)
    }

    override fun decode(source: InputStream, width: Int, height: Int, options: Options): Resource<Bitmap>? {
        val bytes = source.readBytes()
        val bmp = SvgImageDecoder.decode(bytes, width, height) ?: return null
        return BitmapResource.obtain(bmp, bitmapPool)
    }
}
