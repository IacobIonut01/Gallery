package com.dot.gallery.core.presentation.components

import android.graphics.drawable.Drawable
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.dot.gallery.core.presentation.components.util.advancedShadow
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.ui.theme.Dimens
import com.dot.gallery.ui.theme.Shapes
import java.io.File
import kotlin.math.abs
import kotlin.math.withSign

@Composable
fun MediaComponent(
    media: Media,
    preloadRequestBuilder: RequestBuilder<Drawable>,
    onItemClick: (Media) -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .border(
                width = .2.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = Shapes.small
            )
            .clip(Shapes.small)
            .background(
                color = MaterialTheme.colorScheme.surface,
            )
            .clickable {
                onItemClick(media)
            },
    ) {
        MediaImage(media = media, preloadRequestBuilder)
        if (media.duration != null) {
            VideoDurationHeader(media = media)
        }
    }
}

@Composable
fun MediaPreviewComponent(
    media: Media,
    scrollEnabled: MutableState<Boolean>,
    playWhenReady: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = Color.Black,
            ),
    ) {
        if (media.duration != null) {
            VideoPlayer(media, playWhenReady)
        } else {
            ZoomablePagerImage(
                modifier = Modifier
                    .fillMaxSize(),
                media = media,
                scrollEnabled = scrollEnabled
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    media: Media,
    playWhenReady: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context)
            .build().apply {
                val defaultDataSourceFactory = DefaultDataSource.Factory(context)
                val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(
                    context,
                    defaultDataSourceFactory
                )
                val source = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(media.uri))

                prepare(source)
            }
    }

    exoPlayer.playWhenReady = playWhenReady
    exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
    exoPlayer.repeatMode = Player.REPEAT_MODE_ONE

    DisposableEffect(
        AndroidView(modifier = Modifier.clickable {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                exoPlayer.play()
            }

        }, factory = {
            PlayerView(context).apply {
                //hideController()
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                player = exoPlayer
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            }
        })
    ) {
        onDispose {
            exoPlayer.release()
        }
    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun ZoomablePagerImage(
    modifier: Modifier = Modifier,
    media: Media,
    scrollEnabled: MutableState<Boolean>,
    minScale: Float = 1f,
    maxScale: Float = 5f,
    contentScale: ContentScale = ContentScale.FillWidth,
    isRotation: Boolean = false,
) {
    var targetScale by remember { mutableStateOf(1f) }
    val scale = animateFloatAsState(targetValue = maxOf(minScale, minOf(maxScale, targetScale)))
    var rotationState by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(1f) }
    var offsetY by remember { mutableStateOf(1f) }
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
    Box(
        modifier = modifier
            .clip(RectangleShape)
            .background(Color.Transparent)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { },
                onDoubleClick = {
                    if (targetScale >= 2f) {
                        targetScale = 1f
                        offsetX = 1f
                        offsetY = 1f
                        scrollEnabled.value = true
                    } else targetScale = 3f
                },
            )
            .pointerInput(Unit) {
                forEachGesture {
                    awaitPointerEventScope {
                        awaitFirstDown()
                        do {
                            val event = awaitPointerEvent()
                            val zoom = event.calculateZoom()
                            targetScale *= zoom
                            val offset = event.calculatePan()
                            if (targetScale <= 1) {
                                offsetX = 1f
                                offsetY = 1f
                                targetScale = 1f
                                scrollEnabled.value = true
                            } else {
                                offsetX += offset.x
                                offsetY += offset.y
                                if (zoom > 1) {
                                    scrollEnabled.value = false
                                    rotationState += event.calculateRotation()
                                }
                                val imageWidth = screenWidthPx * scale.value
                                val borderReached = imageWidth - screenWidthPx - 2 * abs(offsetX)
                                scrollEnabled.value = borderReached <= 0
                                if (borderReached < 0) {
                                    offsetX = ((imageWidth - screenWidthPx) / 2f).withSign(offsetX)
                                    if (offset.x != 0f) offsetY -= offset.y
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
            }

    ) {
        GlideImage(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    this.scaleX = scale.value
                    this.scaleY = scale.value
                    if (isRotation) {
                        rotationZ = rotationState
                    }
                    this.translationX = offsetX
                    this.translationY = offsetY
                },
            model = File(media.path),
            contentDescription = media.label,
            contentScale = contentScale,
        )
    }
}

@Composable
fun BoxScope.VideoDurationHeader(media: Media) {
    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(all = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .advancedShadow(
                    cornersRadius = 2.dp,
                    shadowBlurRadius = 6.dp,
                    alpha = 0.1f,
                    offsetY = (-2).dp
                ),
            text = media.formatTime(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
        Spacer(modifier = Modifier.size(2.dp))
        Image(
            modifier = Modifier
                .size(16.dp)
                .advancedShadow(
                    cornersRadius = 2.dp,
                    shadowBlurRadius = 6.dp,
                    alpha = 0.1f,
                    offsetY = (-2).dp
                ),
            imageVector = Icons.Rounded.PlayCircle,
            colorFilter = ColorFilter.tint(color = Color.White),
            contentDescription = "Video"
        )
    }
}

/**
 * @param model -> Data source to display the image
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MediaImage(media: Media, preloadRequestBuilder: RequestBuilder<Drawable>) {
    GlideImage(
        modifier = Modifier
            .aspectRatio(1f)
            .size(Dimens.Photo()),
        model = File(media.path),
        contentDescription = media.label,
        contentScale = ContentScale.Crop,
    ) {
        it.thumbnail(preloadRequestBuilder)
    }
}
