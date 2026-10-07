package com.dot.gallery.feature.editor

import com.dot.gallery.core.decoder.format.ImageReencoder
import com.dot.gallery.feature.editor.components.develop.RawSaveFormat
import kotlinx.coroutines.sync.Mutex

object EditorOutputPolicy {
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