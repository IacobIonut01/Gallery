package com.dot.gallery.core.presentation.util

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.SharedContentConfig
import androidx.compose.animation.core.ExperimentalDeferredTransitionApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.Media
import com.dot.gallery.core.LocalViewerDismissBridge
import com.dot.gallery.core.ui.settings.rememberPreference
import com.dot.gallery.domain.settings.SettingsKeys
import com.dot.gallery.core.ViewerDismissBridge




/**
 * Deterministic bounds spec for every shared element. The default spring gets clipped by short
 * exit transitions (the bounds animation stops when the transition ends); a tween slightly
 * shorter than the container's exit fade guarantees the return flight completes.
 */
val SharedElementBoundsTransform = BoundsTransform { initial, target ->
    tween(durationMillis = 280, easing = FastOutSlowInEasing)
}

sealed interface MediaSharedElementKey {
    data class MediaKey(val id: Long) : MediaSharedElementKey
    data class AlbumKey(val id: Long) : MediaSharedElementKey
    data class CategoryKey(val id: Long) : MediaSharedElementKey
    data class StoryCardKey(val id: Long) : MediaSharedElementKey
}

context(namedSharedTransitionScope: SharedTransitionScope)
@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun <T: Media> Modifier.mediaSharedElement(
    allowAnimation: Boolean = true,
    media: T,
    animatedVisibilityScope: AnimatedVisibilityScope,
    permitTransformDuringDeferredTransition: Boolean = false,
): Modifier = mediaSharedElement(
    allowAnimation = allowAnimation,
    key = MediaSharedElementKey.MediaKey(media.id),
    animatedVisibilityScope = animatedVisibilityScope,
    permitTransformDuringDeferredTransition = permitTransformDuringDeferredTransition,
)

context(namedSharedTransitionScope: SharedTransitionScope)
@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.mediaSharedElement(
    allowAnimation: Boolean = true,
    album: Album,
    animatedVisibilityScope: AnimatedVisibilityScope
): Modifier = mediaSharedElement(allowAnimation = allowAnimation, key = MediaSharedElementKey.AlbumKey(album.id), animatedVisibilityScope = animatedVisibilityScope)

context(namedSharedTransitionScope: SharedTransitionScope)
@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.categorySharedElement(
    allowAnimation: Boolean = true,
    categoryId: Long,
    animatedVisibilityScope: AnimatedVisibilityScope
): Modifier = mediaSharedElement(allowAnimation = allowAnimation, key = MediaSharedElementKey.CategoryKey(categoryId), animatedVisibilityScope = animatedVisibilityScope)

context(namedSharedTransitionScope: SharedTransitionScope)
@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.storyCardSharedElement(
    allowAnimation: Boolean = true,
    cardId: Long,
    animatedVisibilityScope: AnimatedVisibilityScope,
    permitTransformDuringDeferredTransition: Boolean = false,
): Modifier = mediaSharedElement(
    allowAnimation = allowAnimation,
    key = MediaSharedElementKey.StoryCardKey(cardId),
    animatedVisibilityScope = animatedVisibilityScope,
    permitTransformDuringDeferredTransition = permitTransformDuringDeferredTransition,
)

/**
 * Config that lets an overlay viewer's armed swipe-dismiss suppress the matching entries.
 * While a drag defers the overlay transition, both the source cell and the viewer element
 * drop out of the match: the media then renders in place inside the offset container and
 * simply rides the finger, while the committed return flight is driven manually by
 * [ViewerDismissBridge.flight] — no bounds animation tracks the gesture.
 */
@OptIn(ExperimentalDeferredTransitionApi::class)
private class DismissAwareSharedContentConfig(
    private val dismissBridge: ViewerDismissBridge?,
    private val permitTransform: Boolean,
) : SharedContentConfig {
    override val SharedTransitionScope.SharedContentState.isEnabled: Boolean
        get() = dismissBridge?.suppressedElementKey != key

    /**
     * Default true keeps a suppressed entry enabled while its bounds animation exists — which
     * would let the source cell keep re-configuring the match during the drag-defer
     * (isAnimating survives until the DeferredAnimation is recreated). Returning false makes
     * the suppression take effect immediately.
     */
    override val shouldKeepEnabledForOngoingAnimation: Boolean
        get() = false

    override val permitTransformDuringDeferredTransition: Boolean
        get() = permitTransform
}

context(namedSharedTransitionScope: SharedTransitionScope)
@Composable
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalDeferredTransitionApi::class)
private fun Modifier.mediaSharedElement(
    allowAnimation: Boolean = true,
    key: MediaSharedElementKey,
    animatedVisibilityScope: AnimatedVisibilityScope,
    permitTransformDuringDeferredTransition: Boolean = false,
): Modifier = with(namedSharedTransitionScope) {
    val shouldAnimate by rememberPreference(SettingsKeys.sharedElements)
    // Source elements (grid cells, story cards) keep the flag false so they stay pinned during
    // the viewer's deferred swipe-dismiss instead of sliding with the mutating container. The
    // viewer element inside the overlay opts in so a system predictive-back transform still
    // applies to it.
    val dismissBridge = LocalViewerDismissBridge.current
    val boundsModifier = sharedBounds(
        sharedContentState = rememberSharedContentState(
            key = key,
            config = remember(dismissBridge, permitTransformDuringDeferredTransition) {
                DismissAwareSharedContentConfig(dismissBridge, permitTransformDuringDeferredTransition)
            },
        ),
        animatedVisibilityScope = animatedVisibilityScope,
        boundsTransform = SharedElementBoundsTransform,
    )
    // Source entries keep their root bounds reported so a committed dismiss can fly the media
    // back to the live cell rect — even after the cell re-laid out under the open viewer.
    val reporter =
        if (!permitTransformDuringDeferredTransition && dismissBridge != null) {
            Modifier.onGloballyPositioned { coords ->
                dismissBridge.cellBounds[key] = coords.boundsInRoot()
            }
        } else Modifier
    return remember(shouldAnimate, allowAnimation) {
        if (shouldAnimate && allowAnimation) boundsModifier else Modifier
    }.then(reporter)
}
