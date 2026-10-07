package com.dot.gallery.core.decryption

import android.content.Context
import androidx.collection.LruCache
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import com.dot.gallery.core.injection.Singleton
import com.dot.gallery.core.metrics.MetricsCollector
import com.dot.gallery.data.data_source.KeychainHolder
import com.dot.gallery.domain.uri.path

/** Simple file-based sidecar metadata cache (lazy). */
@Singleton
class MediaMetadataSidecarCache @Inject constructor(
    private val context: Context
) {
    private val dir by lazy { File(context.cacheDir, "meta_sidecar").apply { mkdirs() } }

    fun read(key: String): MediaMetadataCacheEntry? {
        val f = File(dir, key)
        if (!f.exists()) return null
        return try {
            val text = f.readText()
            val parts = text.split('|')
            MediaMetadataCacheEntry(
                path = parts.getOrNull(0) ?: return null,
                mimeType = parts.getOrNull(1) ?: return null,
                width = parts.getOrNull(2)?.toIntOrNull(),
                height = parts.getOrNull(3)?.toIntOrNull(),
                durationMs = parts.getOrNull(4)?.toLongOrNull()
            )
        } catch (_: Throwable) { null }
    }

    fun write(entry: MediaMetadataCacheEntry) {
        val key = hash(entry.path)
        val f = File(dir, key)
        runCatching {
            f.writeText(
                listOf(
                    entry.path, entry.mimeType,
                    entry.width?.toString() ?: "",
                    entry.height?.toString() ?: "",
                    entry.durationMs?.toString() ?: ""
                ).joinToString("|")
            )
        }
    }

    fun keyForFile(file: File): String = hash(file.path)

    private fun hash(path: String): String {
        val md = MessageDigest.getInstance("MD5")
        md.update(path.encodeToByteArray())
        return md.digest().joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    }
}
