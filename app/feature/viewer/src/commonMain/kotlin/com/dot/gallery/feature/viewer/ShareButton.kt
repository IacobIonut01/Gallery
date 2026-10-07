package com.dot.gallery.feature.viewer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import com.dot.gallery.domain.model.Media
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.share


/**
 * Share button; the share itself (plain vs decrypt-then-share for vault media)
 * is handled app-side via [onShare].
 */
@Composable
fun <T : Media> ShareButton(
    media: T,
    enabled: Boolean,
    followTheme: Boolean = false,
    onShare: (T) -> Unit
) {
    MediaViewButton(
        currentMedia = media,
        imageVector = Icons.Outlined.Share,
        followTheme = followTheme,
        title = stringResource(Res.string.share),
        enabled = enabled,
        onItemClick = onShare
    )
}
