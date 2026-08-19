package com.dot.gallery.feature_node.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.dot.gallery.core.DefaultEventHandler
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.ui.theme.GalleryTheme

/** A stable, preference-independent host for screenshots and interactive previews. */
@Composable
fun PreviewHost(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val eventHandler = remember { DefaultEventHandler() }
    CompositionLocalProvider(LocalEventHandler provides eventHandler) {
        GalleryTheme(
            darkTheme = darkTheme,
            dynamicColor = false,
            ignoreUserPreference = true,
            content = content,
        )
    }
}
