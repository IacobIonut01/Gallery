package com.dot.gallery.feature.viewer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.Composable
import com.dot.gallery.domain.model.Media
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.hide


/**
 * Hide-in-vault button; the vault picker/encrypt flow and MediaStore delete
 * requests are app-side, reached via [onHide].
 */
@Composable
fun <T : Media> HideButton(
    media: T,
    enabled: Boolean,
    followTheme: Boolean = false,
    onHide: (T) -> Unit
) {
    MediaViewButton(
        currentMedia = media,
        imageVector = Icons.Outlined.Lock,
        followTheme = followTheme,
        enabled = enabled,
        title = stringResource(Res.string.hide),
        onItemClick = onHide
    )
}
