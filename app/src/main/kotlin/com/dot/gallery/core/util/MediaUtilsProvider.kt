package com.dot.gallery.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.dot.gallery.core.Settings
import android.graphics.drawable.Drawable
import android.graphics.drawable.ColorDrawable
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.signature.ObjectKey
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.LocalMediaDistributor
import com.dot.gallery.core.LocalMediaHandler
import com.dot.gallery.core.LocalMediaSelector
import com.dot.gallery.core.MediaDistributor
import com.dot.gallery.core.MediaHandler
import com.dot.gallery.core.MediaSelector
import com.dot.gallery.core.image.thumbnail.LocalThumbnailMotion
import com.dot.gallery.core.image.thumbnail.ThumbnailTelemetry
import com.dot.gallery.core.presentation.components.LocalMediaImageRenderer
import com.dot.gallery.core.presentation.components.MediaImageRenderer
import com.dot.gallery.feature_node.domain.util.EventHandler

/**
 * Software-decoded image formats with no hardware decoder. They are expensive to decode, so the
 * timeline renderer avoids the extra 0.4x thumbnail pass for them (which would decode twice).
 */
private val HEAVY_CODEC_EXTENSIONS = listOf(
    ".heic", ".heif", ".avif", ".avis", ".jxl", ".tiff", ".tif", ".psd", ".jp2", ".j2k"
)

/**
 * Pixel bound for the cheap MOTION tier loaded during a fling (#1076 Phase 3). Small enough to
 * decode quickly (and to be served by the platform MediaStore thumbnail fast path) yet crisp
 * enough to avoid obvious blur on dense grids. Reused as the REFINED request's placeholder.
 */
private const val THUMBNAIL_MOTION_PX = 256

/**
 * Default [MediaImageRenderer] that uses GlideImage with full caching,
 * thumbnail generation, GIF animation, and cache-invalidation signatures.
 *
 * GIF thumbnail animation is controlled by the [Settings.Misc.rememberAllowGifAnimation]
 * preference. The [signature] parameter (typically the Media or Album object) is used both
 * for Glide cache invalidation and for GIF filename detection via `toString()`.
 */
@OptIn(ExperimentalGlideComposeApi::class)
val GlideMediaImageRenderer = object : MediaImageRenderer {
    @Composable
    override fun RenderImage(
        modifier: Modifier,
        model: Any?,
        contentScale: ContentScale,
        contentDescription: String?,
        signature: Any?
    ) {
        val allowGifAnimation by Settings.Misc.rememberAllowGifAnimation()
        // Phase 3 (#1076): scroll-aware tier. Null (surfaces without motion state) => idle/refined.
        val isMoving = LocalThumbnailMotion.current?.value == true
        val signatureStr = signature?.toString() ?: ""
        // Animated formats stay static during motion; they may animate only once the list is idle.
        val isGif = allowGifAnimation && !isMoving && signatureStr.contains(".gif", ignoreCase = true)
        val isAnimatable = allowGifAnimation && !isMoving && (
            signatureStr.contains(".avif", ignoreCase = true) ||
            signatureStr.contains(".apng", ignoreCase = true)
        )
        // Heavy software codecs (no hardware decode) are expensive to decode. The idle refined pass
        // skips the extra thumbnail sub-request for them so they decode once, not twice.
        val isHeavyCodec = HEAVY_CODEC_EXTENSIONS.any { signatureStr.contains(it, ignoreCase = true) }
        val tier = if (isMoving) "MOTION" else "REFINED"
        GlideImage(
            modifier = modifier,
            model = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            loading = placeholder(0x4D444444.toDrawable()),
            failure = placeholder(0x33444444.toDrawable()),
            requestBuilderTransform = {
                val base = it.centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL)
                // Cheap MOTION-tier sub-request: small, static, stable key. It is used both as the
                // standalone request during a fling and as the idle refined request's thumbnail
                // placeholder, so the same bitmap is decoded once and reused (no flicker/re-decode).
                var motion = base.clone().override(THUMBNAIL_MOTION_PX)
                if (signature != null) motion = motion.signature(ObjectKey(signatureStr))

                var request = if (isMoving) {
                    // Moving: load only the cheap tier — no full-size sibling, no animation.
                    motion
                } else {
                    var refined = base
                    if (signature != null) refined = refined.signature(ObjectKey(signatureStr))
                    // Show the cached motion bitmap instantly while the refined image decodes.
                    if (!isHeavyCodec) refined = refined.thumbnail(motion)
                    refined
                }
                // Phase 1 (#1076): attach the bounded telemetry listener (staging/debug only;
                // null in release so this is a no-op there).
                ThumbnailTelemetry.listener(surface = "grid", tier = tier)?.let {
                    request = request.addListener(it)
                }
                if (isGif) {
                    request = request.decode(GifDrawable::class.java)
                } else if (isAnimatable) {
                    request = request.decode(Drawable::class.java)
                }
                request
            }
        )
    }
}

@Composable
fun SetupMediaProviders(
    eventHandler: EventHandler,
    mediaDistributor: MediaDistributor,
    mediaHandler: MediaHandler,
    mediaSelector: MediaSelector,
    mediaImageRenderer: MediaImageRenderer = GlideMediaImageRenderer,
    content: @Composable () -> Unit
) = CompositionLocalProvider(
    LocalEventHandler provides eventHandler,
    LocalMediaDistributor provides mediaDistributor,
    LocalMediaHandler provides mediaHandler,
    LocalMediaSelector provides mediaSelector,
    LocalMediaImageRenderer provides mediaImageRenderer,
    content = content
)
