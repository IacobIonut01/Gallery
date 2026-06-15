/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder.format

import android.graphics.Canvas
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import androidx.core.graphics.createBitmap
import com.caverock.androidsvg.SVG
import java.io.ByteArrayInputStream

/**
 * Rasterizes SVG documents to a [Bitmap] using AndroidSVG. Glide (used for the timeline grid)
 * has no SVG support, so SVG thumbnails would otherwise fail to load even though the Sketch-based
 * media viewer renders them fine.
 */
object SvgImageDecoder {

    private const val TAG = "SvgImageDecoder"
    private const val DEFAULT_SIZE = 512

    /**
     * Long-edge cap used when rendering an SVG as the base bitmap for subsampling. Larger values
     * give crisper zoom at the cost of memory; 4096 keeps a full RGBA bitmap under ~64MB.
     */
    const val REGION_MAX_DIM = 4096

    /** Computes the raster dimensions [decode] would produce for the given bounds, without rendering. */
    fun renderSize(bytes: ByteArray, reqW: Int, reqH: Int): Size? {
        return try {
            val svg = SVG.getFromInputStream(ByteArrayInputStream(bytes))
            val (w, h) = computeTarget(svg, reqW, reqH)
            Size(w, h)
        } catch (e: Throwable) {
            Log.e(TAG, "renderSize failed: ${e.message}")
            null
        }
    }

    fun decode(bytes: ByteArray, reqW: Int, reqH: Int): Bitmap? {
        return try {
            val svg = SVG.getFromInputStream(ByteArrayInputStream(bytes))
            val (targetW, targetH) = computeTarget(svg, reqW, reqH)

            svg.setDocumentWidth(targetW.toFloat())
            svg.setDocumentHeight(targetH.toFloat())

            val bitmap = createBitmap(targetW, targetH)
            val canvas = Canvas(bitmap)
            svg.renderToCanvas(canvas)
            bitmap
        } catch (e: Throwable) {
            Log.e(TAG, "decode failed: ${e.message}", e)
            null
        }
    }

    /** Aspect-preserving target dimensions for the given bounds (0 = unconstrained). */
    private fun computeTarget(svg: SVG, reqW: Int, reqH: Int): Pair<Int, Int> {
        val aspect = svg.documentAspectRatio // width / height, -1 if unknown
        val intrinsicW = svg.documentWidth
        val intrinsicH = svg.documentHeight

        val maxW = if (reqW > 0) reqW else if (intrinsicW > 0) intrinsicW.toInt() else DEFAULT_SIZE
        val maxH = if (reqH > 0) reqH else if (intrinsicH > 0) intrinsicH.toInt() else DEFAULT_SIZE

        return when {
            aspect > 0 -> {
                if (maxW.toFloat() / maxH > aspect) {
                    (maxH * aspect).toInt().coerceAtLeast(1) to maxH
                } else {
                    maxW to (maxW / aspect).toInt().coerceAtLeast(1)
                }
            }
            else -> maxW to maxH
        }
    }
}
