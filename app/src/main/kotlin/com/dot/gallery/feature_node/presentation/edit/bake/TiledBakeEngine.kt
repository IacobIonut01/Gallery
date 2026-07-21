/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.edit.bake

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import androidx.core.graphics.createBitmap
import com.dot.gallery.core.util.SafeExif
import com.dot.gallery.feature_node.domain.model.editor.Adjustment
import com.dot.gallery.feature_node.domain.model.editor.TileBehavior
import com.dot.gallery.feature_node.domain.model.editor.TileableAdjustment
import kotlin.math.max
import kotlin.math.min

/**
 * Memory-bounded, full-resolution bake that processes the source in horizontal strips via
 * [BitmapRegionDecoder] instead of decoding the whole original into one bitmap. This avoids the
 * largest single allocation on the input side (a 100 MP source ≈ 400 MB ARGB) while producing a
 * pixel-identical result to [EditReplay] for the recipes it supports.
 *
 * Deliberately conservative — it only engages when correctness is guaranteed, otherwise it returns
 * `null` so the caller falls back to the verified whole-image [EditReplay]:
 *  - **No geometry ops** in the recipe (crop/rotate/flip/borders change dimensions/orientation and
 *    need inverse-mapping — handled by the whole-image path for now). Output dims == source dims.
 *  - **Normal EXIF orientation** — [BitmapRegionDecoder] ignores EXIF, whereas the proxy (and thus
 *    the recorded recipe) was built on the Glide-rotated image; a non-normal orientation would make
 *    the tiled pixels disagree with the recipe.
 *  - **Region-decodable source** — standard JPEG/PNG/WebP (and HEIF/AVIF on API 31+). Custom
 *    formats (JXL/PSD/RAW/…) throw and fall back.
 *
 * Supported per-strip ops: per-pixel colour (independent), local kernels (Sharpen/Denoise/Edges —
 * each strip is decoded with a halo equal to the summed kernel radii so seams match a full pass),
 * analytic ops (Vignette) and Markup — the latter two via [TileableAdjustment.applyTile] so they
 * are rendered from the full-image geometry.
 */
object TiledBakeEngine {

    /** Interior strip height in source pixels; halo is added on top when decoding. */
    private const val STRIP_HEIGHT = 512

    /** Native scanline encoders the streaming bake can target. */
    enum class StreamFormat { JPEG, PNG }

    /**
     * Cheap eligibility check (no full decode): the recipe must have no geometry ops and the source
     * must have a normal EXIF orientation. Region-decodability is only known once the decoder is
     * opened, so a `true` here still allows the actual bake to fail and fall back.
     */
    fun isStreamEligible(context: Context, uri: Uri, adjustments: List<Adjustment>): Boolean {
        if (adjustments.isEmpty()) return false
        if (adjustments.any { it.tileBehavior is TileBehavior.Geometry }) return false
        return hasNormalOrientation(context, uri)
    }

    /** Bakes the recipe onto the full-resolution source into a single output bitmap, in strips. */
    fun bake(context: Context, uri: Uri, adjustments: List<Adjustment>): Bitmap? {
        if (!isStreamEligible(context, uri, adjustments)) return null
        return withDecoder(context, uri) { decoder ->
            val fullWidth = decoder.width
            val fullHeight = decoder.height
            if (fullWidth <= 0 || fullHeight <= 0) return@withDecoder null
            val output = createBitmap(fullWidth, fullHeight)
            val outCanvas = Canvas(output)
            val ok = processStrips(decoder, adjustments, fullWidth, fullHeight) { tile, interiorTop, stripHeight ->
                outCanvas.drawBitmap(
                    tile,
                    Rect(0, interiorTop, fullWidth, interiorTop + stripHeight),
                    Rect(0, currentOutY, fullWidth, currentOutY + stripHeight),
                    null
                )
                true
            }
            if (ok) output else { output.recycle(); null }
        }
    }

    /**
     * Bakes the recipe onto the full-resolution source and streams the result **one strip at a
     * time** into the native scanline encoder writing to [fd] — so the whole output image is never
     * held in RAM. Returns `false` (before writing anything meaningful) when the source can't be
     * region-decoded or the encoder can't open, so the caller can fall back to the whole-bitmap
     * path. [fd] must be a writable file descriptor; the native side dup()s it.
     */
    fun bakeToStream(
        context: Context,
        uri: Uri,
        adjustments: List<Adjustment>,
        format: StreamFormat,
        quality: Int,
        fd: Int,
        onProgress: ((Float) -> Unit)? = null,
    ): Boolean {
        if (!NativeImageEncoder.isAvailable) return false
        if (!isStreamEligible(context, uri, adjustments)) return false
        val result = withDecoder(context, uri) { decoder ->
            val fullWidth = decoder.width
            val fullHeight = decoder.height
            if (fullWidth <= 0 || fullHeight <= 0) return@withDecoder false

            val handle = when (format) {
                StreamFormat.JPEG -> NativeImageEncoder.nativeJpegOpen(fd, fullWidth, fullHeight, quality)
                StreamFormat.PNG -> NativeImageEncoder.nativePngOpen(fd, fullWidth, fullHeight)
            }
            if (handle == 0L) return@withDecoder false

            // Reusable ARGB row buffer sized to the tallest strip.
            val rowBuf = IntArray(fullWidth * STRIP_HEIGHT)
            val ok = processStrips(decoder, adjustments, fullWidth, fullHeight, onProgress) { tile, interiorTop, stripHeight ->
                tile.getPixels(rowBuf, 0, fullWidth, 0, interiorTop, fullWidth, stripHeight)
                when (format) {
                    StreamFormat.JPEG -> NativeImageEncoder.nativeJpegWriteRows(handle, rowBuf, stripHeight)
                    StreamFormat.PNG -> NativeImageEncoder.nativePngWriteRows(handle, rowBuf, stripHeight)
                }
            }
            val finished = when (format) {
                StreamFormat.JPEG -> NativeImageEncoder.nativeJpegFinish(handle)
                StreamFormat.PNG -> NativeImageEncoder.nativePngFinish(handle)
            }
            ok && finished
        }
        return result ?: false
    }

    /** Square tile size for grid HEIC/AVIF encode. */
    private const val TILE_SIZE = 512

    /**
     * Bakes the recipe onto the full-resolution source and encodes it as a TILED HEIC/AVIF via
     * libheif's grid API — one 512×512 tile at a time, so the whole output image is never held in
     * RAM. [format] is [NativeHeifEncoder.FORMAT_HEIC] or `FORMAT_AVIF`. Returns false (before
     * meaningful writes) when ineligible/unavailable so the caller can fall back.
     */
    fun bakeToHeif(
        context: Context,
        uri: Uri,
        adjustments: List<Adjustment>,
        format: Int,
        quality: Int,
        fd: Int,
        onProgress: ((Float) -> Unit)? = null,
    ): Boolean {
        if (!NativeHeifEncoder.isAvailable) return false
        if (!isStreamEligible(context, uri, adjustments)) return false
        val result = withDecoder(context, uri) { decoder ->
            val fullWidth = decoder.width
            val fullHeight = decoder.height
            if (fullWidth <= 0 || fullHeight <= 0) return@withDecoder false

            val handle = NativeHeifEncoder.nativeOpen(
                fd, fullWidth, fullHeight, TILE_SIZE, TILE_SIZE, format, quality
            )
            if (handle == 0L) return@withDecoder false

            val tileBuf = IntArray(TILE_SIZE * TILE_SIZE)
            val ok = process2DTiles(decoder, adjustments, fullWidth, fullHeight, onProgress) {
                    tile, interiorLeft, interiorTop, tileW, tileH, tileX, tileY ->
                tile.getPixels(tileBuf, 0, tileW, interiorLeft, interiorTop, tileW, tileH)
                NativeHeifEncoder.nativeEncodeTile(handle, tileBuf, tileW, tileH, tileX, tileY)
            }
            val finished = NativeHeifEncoder.nativeFinish(handle)
            ok && finished
        }
        return result ?: false
    }

    /**
     * Decodes [decoder] as a 2D grid of [TILE_SIZE] tiles (each with a kernel halo on all sides),
     * applies [adjustments] to every tile, then hands the interior region to [sink] with its grid
     * position. Returns true only if every tile decoded and the sink accepted every tile.
     */
    private fun process2DTiles(
        decoder: BitmapRegionDecoder,
        adjustments: List<Adjustment>,
        fullWidth: Int,
        fullHeight: Int,
        onProgress: ((Float) -> Unit)? = null,
        sink: (tile: Bitmap, interiorLeft: Int, interiorTop: Int, tileW: Int, tileH: Int, tileX: Int, tileY: Int) -> Boolean,
    ): Boolean {
        val halo = adjustments.sumOf { adj ->
            (adj.tileBehavior as? TileBehavior.Kernel)?.radius ?: 0
        } + KERNEL_HALO_MARGIN
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }

        val columns = (fullWidth + TILE_SIZE - 1) / TILE_SIZE
        val rows = (fullHeight + TILE_SIZE - 1) / TILE_SIZE

        for (ty in 0 until rows) {
            for (tx in 0 until columns) {
                val originX = tx * TILE_SIZE
                val originY = ty * TILE_SIZE
                val tileW = min(TILE_SIZE, fullWidth - originX)
                val tileH = min(TILE_SIZE, fullHeight - originY)

                val srcLeft = max(0, originX - halo)
                val srcTop = max(0, originY - halo)
                val srcRight = min(fullWidth, originX + tileW + halo)
                val srcBottom = min(fullHeight, originY + tileH + halo)
                val region = Rect(srcLeft, srcTop, srcRight, srcBottom)

                var tile = decoder.decodeRegion(region, options) ?: return false
                for (adj in adjustments) {
                    val next = if (adj is TileableAdjustment) {
                        adj.applyTile(tile, fullWidth, fullHeight, srcLeft, srcTop)
                    } else {
                        adj.apply(tile)
                    }
                    if (next !== tile) {
                        if (!tile.isRecycled) tile.recycle()
                        tile = next
                    }
                }

                val accepted = sink(tile, originX - srcLeft, originY - srcTop, tileW, tileH, tx, ty)
                if (!tile.isRecycled) tile.recycle()
                if (!accepted) return false
                onProgress?.invoke(((ty * columns + tx + 1).toFloat() / (rows * columns)).coerceIn(0f, 1f))
            }
        }
        return true
    }

    /** Opens a [BitmapRegionDecoder] on [uri], runs [block], and always recycles it. */
    private fun <T> withDecoder(context: Context, uri: Uri, block: (BitmapRegionDecoder) -> T?): T? {
        val pfd = runCatching { context.contentResolver.openFileDescriptor(uri, "r") }
            .getOrNull() ?: return null
        return pfd.use {
            val decoder = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    BitmapRegionDecoder.newInstance(it)
                } else {
                    @Suppress("DEPRECATION")
                    BitmapRegionDecoder.newInstance(it.fileDescriptor, false)
                }
            }.getOrNull() ?: return@use null
            try {
                block(decoder)
            } catch (t: Throwable) {
                null
            } finally {
                @Suppress("DEPRECATION")
                runCatching { decoder.recycle() }
            }
        }
    }

    /** Tracks the current output row while a strip sink runs (avoids a mutable lambda param). */
    private var currentOutY = 0

    /**
     * Decodes [decoder] in horizontal strips (each with a kernel halo), applies [adjustments] to
     * every strip, then hands the interior rows to [sink]. Returns true only if every strip decoded
     * and the sink accepted every strip.
     */
    private fun processStrips(
        decoder: BitmapRegionDecoder,
        adjustments: List<Adjustment>,
        fullWidth: Int,
        fullHeight: Int,
        onProgress: ((Float) -> Unit)? = null,
        sink: (tile: Bitmap, interiorTop: Int, stripHeight: Int) -> Boolean,
    ): Boolean {
        // Halo = summed radii of all kernel ops so multiple sequential convolutions stay seamless.
        val halo = adjustments.sumOf { adj ->
            (adj.tileBehavior as? TileBehavior.Kernel)?.radius ?: 0
        } + KERNEL_HALO_MARGIN

        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }

        var y = 0
        while (y < fullHeight) {
            val stripHeight = min(STRIP_HEIGHT, fullHeight - y)
            val srcTop = max(0, y - halo)
            val srcBottom = min(fullHeight, y + stripHeight + halo)
            val region = Rect(0, srcTop, fullWidth, srcBottom)

            var tile = decoder.decodeRegion(region, options) ?: return false

            for (adj in adjustments) {
                val next = if (adj is TileableAdjustment) {
                    adj.applyTile(tile, fullWidth, fullHeight, 0, srcTop)
                } else {
                    adj.apply(tile)
                }
                if (next !== tile) {
                    if (!tile.isRecycled) tile.recycle()
                    tile = next
                }
            }

            val interiorTop = y - srcTop
            currentOutY = y
            val accepted = sink(tile, interiorTop, stripHeight)
            if (!tile.isRecycled) tile.recycle()
            if (!accepted) return false

            y += stripHeight
            onProgress?.invoke((y.toFloat() / fullHeight).coerceIn(0f, 1f))
        }
        return true
    }

    /** True when the source has no (or a normal/undefined) EXIF orientation. */
    private fun hasNormalOrientation(context: Context, uri: Uri): Boolean {
        // Seekable-FD ExifInterface (via SafeExif): an InputStream-backed one buffers up to the strip
        // offset and OOMs on large 16-bit TIFFs. Unreadable orientation is treated as normal.
        val orientation = SafeExif.orientation(context, uri)
        return orientation == ExifInterface.ORIENTATION_NORMAL ||
                orientation == ExifInterface.ORIENTATION_UNDEFINED
    }

    private const val KERNEL_HALO_MARGIN = 2
}
