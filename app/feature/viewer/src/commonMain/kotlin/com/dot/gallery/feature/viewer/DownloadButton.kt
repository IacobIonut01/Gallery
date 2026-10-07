package com.dot.gallery.feature.viewer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.runtime.Composable
import com.dot.gallery.domain.model.Media
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.download


/**
 * Download (cloud → local) button; the download itself goes through the app's
 * media handler via [onDownload].
 */
@Composable
fun <T : Media> DownloadButton(
    media: T,
    enabled: Boolean,
    followTheme: Boolean = false,
    onDownload: (T) -> Unit
) {
    MediaViewButton(
        currentMedia = media,
        imageVector = Icons.Outlined.Download,
        followTheme = followTheme,
        title = stringResource(Res.string.download),
        enabled = enabled,
        onItemClick = onDownload
    )
}
