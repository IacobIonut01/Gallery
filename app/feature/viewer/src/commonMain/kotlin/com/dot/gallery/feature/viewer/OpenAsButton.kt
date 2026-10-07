package com.dot.gallery.feature.viewer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.runtime.Composable
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.isVideo
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.open_with
import com.dot.gallery.feature.viewer.generated.resources.use_as


/**
 * Open-with/use-as button. Videos are labelled "Open with", stills "Use as";
 * firing the platform intent is left to [onOpenAs].
 */
@Composable
fun <T : Media> OpenAsButton(
    media: T,
    enabled: Boolean,
    followTheme: Boolean = false,
    onOpenAs: (T) -> Unit
) {
    MediaViewButton(
        currentMedia = media,
        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
        followTheme = followTheme,
        title = stringResource(
            if (media.isVideo) Res.string.open_with else Res.string.use_as
        ),
        enabled = enabled,
        onItemClick = onOpenAs
    )
}
