package com.dot.gallery.feature.editor

import com.dot.gallery.core.decoder.format.ImageReencoder
import com.dot.gallery.feature.editor.components.develop.RawSaveFormat
import kotlinx.coroutines.sync.Mutex



data class EditorOutputSpec(
    val writeFormat: ImageReencoder.ImageWriteFormat,
    val displayName: String,
)