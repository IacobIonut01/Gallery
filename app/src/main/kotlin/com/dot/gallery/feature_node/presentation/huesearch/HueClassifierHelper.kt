package com.dot.gallery.feature_node.presentation.huesearch

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HueClassifierHelper(
    private val context: Context
) {
    suspend fun classify(entry: Media.UriMedia) = withContext(Dispatchers.Default) {

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
            palette.swatches.maxByOrNull { it.population } ?: throw IllegalArgumentException("Not enough swatches"),
            palette.swatches.maxByOrNull { it.hsl[1] } ?: throw IllegalArgumentException("Not enough swatches")
        )

        val lab1 = DoubleArray(3).also { ColorUtils.colorToLAB(colorPair.first.rgb, it) }
        val lab2 = DoubleArray(3).also { ColorUtils.colorToLAB(colorPair.second.rgb, it) }

        val (gx1, gy1, gz1) = quantizeLab(lab1[0], lab1[1], lab1[2])
        val (gx2, gy2, gz2) = quantizeLab(lab2[0], lab2[1], lab2[2])

        val code1 = Morton3D.encode(gx1, gy1, gz1)
        val code2 = Morton3D.encode(gx2, gy2, gz2)

        return@withContext Media.HueClassifiedMedia(
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
}

// quantize LAB floats to ints for Morton (e.g. 0–100 for L, –128–127 for a/b)
fun quantizeLab(l: Double, a: Double, b: Double): Triple<Int, Int, Int> {
    val qi = (l / 100f * ((1 shl 7) - 1)).toInt().coerceIn(0, (1 shl 7) - 1)
    val qj = ((a + 128f) / 255f * ((1 shl 7) - 1)).toInt().coerceIn(0, (1 shl 7) - 1)
    val qk = ((b + 128f) / 255f * ((1 shl 7) - 1)).toInt().coerceIn(0, (1 shl 7) - 1)
    return Triple(qi, qj, qk)
}

