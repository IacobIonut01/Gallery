package com.dot.gallery.feature.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.readUriOnly
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.favorite


/**
 * Favorite toggle. The caller resolves the effective favorite state (including
 * optimistic overrides) as [isFavorite] and performs the mutation inside
 * [onToggleFavorite] — where activity-result plumbing, distributor overrides and
 * undo all live app-side.
 */
@Composable
fun <T : Media> FavoriteButton(
    media: T,
    isFavorite: Boolean,
    enabled: Boolean,
    followTheme: Boolean = false,
    onToggleFavorite: (T, Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val pulse = remember { Animatable(1f) }
    // "Animate media items" off — the heart flips with no pulse.
    val animationsEnabled = LocalMediaViewerVisualPolicy.current.animationsEnabled
    if (!media.readUriOnly) {
        MediaViewButton(
            currentMedia = media,
            imageVector = if (isFavorite) Icons.Filled.Favorite
            else Icons.Outlined.FavoriteBorder,
            followTheme = followTheme,
            title = stringResource(Res.string.favorite),
            iconModifier = Modifier.graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            },
            enabled = enabled
        ) {
            val target = !isFavorite
            if (animationsEnabled) {
                scope.launch {
                    pulse.animateTo(1.35f, tween(110))
                    pulse.animateTo(
                        1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                }
            }
            onToggleFavorite(it, target)
        }
    }
}
