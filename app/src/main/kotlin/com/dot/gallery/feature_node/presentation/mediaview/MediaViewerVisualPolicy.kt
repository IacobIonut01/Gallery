package com.dot.gallery.feature_node.presentation.mediaview

import androidx.compose.runtime.staticCompositionLocalOf

data class MediaViewerVisualPolicy(
    val allowBlur: Boolean,
) {
    fun usesDarkBackground(isDarkTheme: Boolean): Boolean = allowBlur || isDarkTheme
}

val LocalMediaViewerVisualPolicy = staticCompositionLocalOf {
    MediaViewerVisualPolicy(allowBlur = false)
}
