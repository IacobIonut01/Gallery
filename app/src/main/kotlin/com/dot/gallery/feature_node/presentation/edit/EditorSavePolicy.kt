package com.dot.gallery.feature_node.presentation.edit

import com.dot.gallery.core.decoder.format.ImageReencoder
import com.dot.gallery.feature_node.presentation.edit.components.develop.RawSaveFormat
import kotlinx.coroutines.sync.Mutex

internal data class EditorOutputSpec(
    val writeFormat: ImageReencoder.ImageWriteFormat,
    val displayName: String,
)

internal object EditorOutputPolicy {
    fun copy(sourceMime: String?, sourceLabel: String, forcePng: Boolean): EditorOutputSpec {
        val format = if (forcePng) {
            ImageReencoder.ImageWriteFormat.PNG
        } else {
            ImageReencoder.formatForMime(sourceMime, sourceLabel)
                ?: ImageReencoder.ImageWriteFormat.PNG
        }
        return EditorOutputSpec(
            writeFormat = format,
            displayName = replaceExtension(sourceLabel, format.fileExtension),
        )
    }

    fun rawCopy(sourceLabel: String, format: RawSaveFormat): String =
        replaceExtension(sourceLabel, format.ext, suffix = "_developed")

    private fun replaceExtension(sourceLabel: String, extension: String, suffix: String = ""): String {
        val cleanLabel = sourceLabel.trim().ifBlank { "edited" }
        val extensionIndex = cleanLabel.lastIndexOf('.')
        val base = if (extensionIndex > 0) cleanLabel.substring(0, extensionIndex) else cleanLabel
        return "${base.ifBlank { "edited" }}$suffix.$extension"
    }
}

/** Rejects overlapping saves instead of queueing a second write behind the first one. */
internal class EditorSaveGuard {
    private val mutex = Mutex()

    fun tryAcquire(): Boolean = mutex.tryLock()

    fun release() {
        mutex.unlock()
    }

    suspend fun runIfIdle(block: suspend () -> Unit): Boolean {
        if (!tryAcquire()) return false
        return try {
            block()
            true
        } finally {
            release()
        }
    }
}
