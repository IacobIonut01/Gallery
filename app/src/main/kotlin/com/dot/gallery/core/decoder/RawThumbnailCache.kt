/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder

import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Small, memory-bounded cache of per-option develop thumbnails for the editor's Develop tab. Each
 * entry is a tiny (~[THUMB_DIM]px) half-size demosaic of a single [RawDevelopParams] variant, so
 * the UI can show an accurate "what this option does" preview without re-decoding on every frame.
 *
 * Demosaics are serialized through a single [mutex] so opening a section (which requests several
 * option thumbnails at once) never floods the CPU with parallel full decodes; results are cached
 * by (mediaId + params) and evicted wholesale when the session ends.
 */
object RawThumbnailCache {

    private const val THUMB_DIM = 128
    private const val MAX_ENTRIES = 64

    private val cache = LruCache<String, Bitmap>(MAX_ENTRIES)
    private val mutex = Mutex()

    private fun keyOf(mediaId: Long, params: RawDevelopParams): String = "$mediaId:${params.hashCode()}"

    /** Cached thumbnail for this exact option, or null when not yet computed. */
    fun peek(mediaId: Long, params: RawDevelopParams): Bitmap? = cache.get(keyOf(mediaId, params))

    /**
     * Returns the cached thumbnail for [params], computing it (half-size demosaic + tone, then
     * downscaled to [THUMB_DIM]) off the main thread if absent. Returns null when native RAW is
     * unavailable or the decode fails.
     */
    suspend fun getOrCompute(
        mediaId: Long,
        bytes: ByteArray,
        userFlip: Int,
        params: RawDevelopParams,
    ): Bitmap? {
        val key = keyOf(mediaId, params)
        cache.get(key)?.let { return it }
        if (!NativeRawDecoder.isAvailable) return null
        return mutex.withLock {
            cache.get(key)?.let { return@withLock it } // recheck after acquiring the lock
            val bmp = withContext(Dispatchers.Default) {
                val base = NativeRawDecoder.demosaic(bytes, params.copy(halfSize = true).baseOnly, userFlip)
                    ?: return@withContext null
                val toned = if (params.hasTone) NativeRawDecoder.applyTone(base, params) ?: base else base
                scaleDown(toned)
            }
            if (bmp != null) cache.put(key, bmp)
            bmp
        }
    }

    /** Drop every cached thumbnail (call when leaving the editor / switching media). */
    fun clear() = cache.evictAll()

    private fun scaleDown(bmp: Bitmap): Bitmap {
        val longest = maxOf(bmp.width, bmp.height)
        if (longest <= THUMB_DIM) return bmp
        val scale = THUMB_DIM.toFloat() / longest
        val w = (bmp.width * scale).toInt().coerceAtLeast(1)
        val h = (bmp.height * scale).toInt().coerceAtLeast(1)
        return bmp.scale(w, h)
    }
}
