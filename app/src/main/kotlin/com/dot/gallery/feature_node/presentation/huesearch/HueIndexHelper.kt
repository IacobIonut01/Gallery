package com.dot.gallery.feature_node.presentation.huesearch

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HueIndexHelper(
    private val context: Context
) {
    suspend fun classify(entry: Media.UriMedia) = withContext(Dispatchers.IO) {
        // load a down‑sampled Bitmap
        val bitmap = context.contentResolver.openInputStream(entry.uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply {
                inSampleSize = 4  // adjust for performance/quality
            })
        } ?: throw IllegalArgumentException("Failed to decode bitmap")

        // extract top‑2 swatches in LAB
        val palette = Palette.from(bitmap)
            .maximumColorCount(16)
            .generate()
        if (palette.swatches.size < 2) throw IllegalArgumentException("Not enough swatches")

        val colorPair = Pair(
            palette.swatches.maxByOrNull { it.population }
                ?: throw IllegalArgumentException("Not enough swatches"),
            palette.swatches.maxByOrNull { it.hsl[1] }
                ?: throw IllegalArgumentException("Not enough swatches")
        )

        val lab1 = DoubleArray(3).also { ColorUtils.colorToLAB(colorPair.first.rgb, it) }
        val lab2 = DoubleArray(3).also { ColorUtils.colorToLAB(colorPair.second.rgb, it) }

        val (gx1, gy1, gz1) = quantizeLab(lab1[0], lab1[1], lab1[2])
        val (gx2, gy2, gz2) = quantizeLab(lab2[0], lab2[1], lab2[2])

        val code1 = encode(gx1, gy1, gz1)
        val code2 = encode(gx2, gy2, gz2)

        return@withContext Media.HueIndexedMedia(
            id = entry.id,
            uri = entry.uri,
            morton1 = code1,
            morton2 = code2,
            label = entry.label,
            path = entry.path,
            relativePath = entry.relativePath,
            albumID = entry.albumID,
            albumLabel = entry.albumLabel,
            timestamp = entry.timestamp,
            expiryTimestamp = entry.expiryTimestamp,
            takenTimestamp = entry.takenTimestamp,
            fullDate = entry.fullDate,
            mimeType = entry.mimeType,
            favorite = entry.favorite,
            trashed = entry.trashed,
            size = entry.size,
            duration = entry.duration,
            L1 = lab1[0],
            a1 = lab1[1],
            b1 = lab1[2],
            L2 = lab2[0],
            a2 = lab2[1],
            b2 = lab2[2],
        )
    }

    companion object Morton3D {
        /** Spread the low 21 bits of v so there are two zeros between each bit. */
        private fun part1By2(v: Int): Long {
            var x = v.toLong() and 0x1FFFFFL
            x = (x or (x shl 32)) and 0x1F00000000FFFFL
            x = (x or (x shl 16)) and 0x1F0000FF0000FFL
            x = (x or (x shl 8)) and 0x100F00F00F00F00FL
            x = (x or (x shl 4)) and 0x10C30C30C30C30C3L
            x = (x or (x shl 2)) and 0x1249249249249249L
            return x
        }

        /** Compute a 3D Morton code from integer grid coords gx, gy, gz. */
        fun encode(gx: Int, gy: Int, gz: Int): Long {
            return (part1By2(gx)) or
                    (part1By2(gy) shl 1) or
                    (part1By2(gz) shl 2)
        }

        /** Given grid coords and a neighbor radius r, return all neighbor codes within ±r */
        fun neighbors(gx: Int, gy: Int, gz: Int, r: Int = 2): List<Long> {
            val list = ArrayList<Long>()
            for (dx in -r..r) for (dy in -r..r) for (dz in -r..r) {
                list += encode(gx + dx, gy + dy, gz + dz)
            }
            return list
        }

        /** Quantize LAB floats to ints for Morton (e.g. 0–100 for L, –128–127 for a/b) */
        fun quantizeLab(l: Double, a: Double, b: Double): Triple<Int, Int, Int> {
            val qi = (l / 100f * ((1 shl 7) - 1)).toInt().coerceIn(0, (1 shl 7) - 1)
            val qj = ((a + 128f) / 255f * ((1 shl 7) - 1)).toInt().coerceIn(0, (1 shl 7) - 1)
            val qk = ((b + 128f) / 255f * ((1 shl 7) - 1)).toInt().coerceIn(0, (1 shl 7) - 1)
            return Triple(qi, qj, qk)
        }
    }
}