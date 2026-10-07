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

/** Metadata cached to avoid re-decrypting large files just for header info. */
data class MediaMetadataCacheEntry(
    val path: String,
    val mimeType: String,
    val width: Int?,
    val height: Int?,
    val durationMs: Long?
)