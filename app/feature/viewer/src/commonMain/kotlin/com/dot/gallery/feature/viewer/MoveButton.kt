package com.dot.gallery.feature.viewer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.runtime.Composable
import com.dot.gallery.domain.model.Media
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.move


/**
 * Move-to-album button; the target-album sheet is app-side, [onMove] asks the
 * caller to show it.
 */
@Composable
fun <T : Media> MoveButton(
    media: T,
    enabled: Boolean,
    followTheme: Boolean = false,
    onMove: (T) -> Unit
) {
    MediaViewButton(
        currentMedia = media,
        imageVector = Icons.AutoMirrored.Outlined.DriveFileMove,
        followTheme = followTheme,
        title = stringResource(Res.string.move),
        enabled = enabled,
        onItemClick = onMove
    )
}
