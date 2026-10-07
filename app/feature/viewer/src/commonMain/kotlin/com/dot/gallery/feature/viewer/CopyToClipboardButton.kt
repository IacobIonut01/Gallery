package com.dot.gallery.feature.viewer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.runtime.Composable
import com.dot.gallery.domain.model.Media
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.copy_to_clipboard


/**
 * Copy-media-to-clipboard button; the clipboard write is platform-specific so
 * [onCopyToClipboard] hands the media to the app-side implementation.
 */
@Composable
fun <T : Media> CopyToClipboardButton(
    media: T,
    enabled: Boolean,
    followTheme: Boolean = false,
    onCopyToClipboard: (T) -> Unit
) {
    MediaViewButton(
        currentMedia = media,
        imageVector = Icons.Outlined.ContentCopy,
        followTheme = followTheme,
        title = stringResource(Res.string.copy_to_clipboard),
        enabled = enabled,
        onItemClick = onCopyToClipboard
    )
}
