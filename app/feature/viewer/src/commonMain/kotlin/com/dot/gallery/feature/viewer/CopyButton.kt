package com.dot.gallery.feature.viewer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.runtime.Composable
import com.dot.gallery.domain.model.Media
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.copy


/**
 * Copy-to-album button; the target-album sheet is app-side, [onCopy] asks the
 * caller to show it.
 */
@Composable
fun <T : Media> CopyButton(
    media: T,
    enabled: Boolean,
    followTheme: Boolean = false,
    onCopy: (T) -> Unit
) {
    MediaViewButton(
        currentMedia = media,
        imageVector = Icons.Outlined.CopyAll,
        followTheme = followTheme,
        title = stringResource(Res.string.copy),
        enabled = enabled,
        onItemClick = onCopy
    )
}
