package com.dot.gallery.feature.viewer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import com.dot.gallery.domain.model.Media
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.edit


/**
 * Edit button; launching an editor (builtin or user-picked external) is an
 * app/platform concern handled through [onEdit].
 */
@Composable
fun <T : Media> EditButton(
    media: T,
    enabled: Boolean,
    followTheme: Boolean = false,
    onEdit: (T) -> Unit
) {
    MediaViewButton(
        currentMedia = media,
        imageVector = Icons.Outlined.Tune,
        followTheme = followTheme,
        title = stringResource(Res.string.edit),
        enabled = enabled,
        onItemClick = onEdit
    )
}
