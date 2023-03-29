package com.dot.gallery.core.presentation.components

import android.graphics.drawable.Drawable
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.bumptech.glide.Priority
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.signature.MediaStoreSignature
import com.dot.gallery.R
import com.dot.gallery.core.Constants.Animation
import com.dot.gallery.core.presentation.components.util.advancedShadow
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.util.formatMinSec
import com.dot.gallery.ui.theme.Black40P
import com.dot.gallery.ui.theme.Dimens
import com.dot.gallery.ui.theme.Shapes
import java.io.File
import kotlin.math.abs
import kotlin.math.withSign

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaComponent(
    media: Media,
    selectionState: MutableState<Boolean>,
    preloadRequestBuilder: RequestBuilder<Drawable>,
    onItemClick: (Media) -> Unit,
    onItemLongClick: (Media) -> Unit,
    isSelected: Boolean,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .border(
                width = .2.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = Shapes.small
            )
            .clip(Shapes.small)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
            )
            .combinedClickable(
                onClick = { onItemClick(media) },
                onLongClick = { onItemLongClick(media) },
            ),
    ) {
        MediaImage(
            media = media,
            preloadRequestBuilder = preloadRequestBuilder,
            selectionState = selectionState,
            isSelected = isSelected,
        )
    }
}

@Composable
fun MediaPreviewComponent(
    media: Media,
    scrollEnabled: MutableState<Boolean>,
    playWhenReady: Boolean,
    onItemClick: () -> Unit,
    videoController: @Composable (ExoPlayer, Long, Long, Int, () -> Unit) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = Color.Black,
            ),
    ) {
        if (media.duration != null) {
            VideoPlayer(
                media = media,
                playWhenReady = playWhenReady,
                videoController = videoController,
                onItemClick = onItemClick
            )
        } else {
            ZoomablePagerImage(
                modifier = Modifier.fillMaxSize(),
                media = media,
                scrollEnabled = scrollEnabled,
                onItemClick = onItemClick
            )
        }
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalFoundationApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    media: Media,
    playWhenReady: Boolean,
    videoController: @Composable (ExoPlayer, Long, Long, Int, () -> Unit) -> Unit,
    onItemClick: () -> Unit
) {

    var totalDuration by remember { mutableStateOf(0L) }

    var currentTime by remember { mutableStateOf(0L) }

    var bufferedPercentage by remember { mutableStateOf(0) }

    var isPlaying by remember { mutableStateOf(true) }
    val context = LocalContext.current

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
    if (playWhenReady)
        exoPlayer.playWhenReady = isPlaying
    exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
    exoPlayer.repeatMode = Player.REPEAT_MODE_ONE

    DisposableEffect(
        AndroidView(modifier = Modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    onItemClick()
                },
            ), factory = {
            PlayerView(context).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH

                player = exoPlayer
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            }
        })
    ) {
        val listener =
            object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    super.onEvents(player, events)
                    totalDuration = player.duration.coerceAtLeast(0L)
                    currentTime = player.currentPosition.coerceAtLeast(0L)
                    bufferedPercentage = player.bufferedPercentage
                    isPlaying = player.isPlaying
                }
            }

        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }
    videoController(exoPlayer, currentTime, totalDuration, bufferedPercentage) {
        isPlaying = !isPlaying
    }
}

@Composable
fun VideoPlayerController(
    paddingValues: PaddingValues,
    player: ExoPlayer,
    currentTime: Long,
    totalTime: Long,
    buffer: Int,
    playToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp)
                .padding(bottom = paddingValues.calculateBottomPadding() + 72.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                modifier = Modifier.width(52.dp),
                text = currentTime.formatMinSec(),
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Box(Modifier.weight(1f)) {
                Slider(
                    modifier = Modifier.fillMaxWidth(),
                    value = buffer.toFloat(),
                    enabled = false,
                    onValueChange = {},
                    valueRange = 0f..100f,
                    colors =
                    SliderDefaults.colors(
                        disabledThumbColor = Color.Transparent,
                        disabledInactiveTrackColor = Color.DarkGray.copy(alpha = 0.4f),
                        disabledActiveTrackColor = Color.Gray
                    )
                )
                Slider(
                    modifier = Modifier.fillMaxWidth(),
                    value = currentTime.toFloat(),
                    onValueChange = { player.seekTo(it.toLong()) },
                    valueRange = 0f..totalTime.toFloat(),
                    colors =
                    SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        activeTickColor = Color.White,
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }
            Text(
                modifier = Modifier.width(52.dp),
                text = totalTime.formatMinSec(),
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        IconButton(
            onClick = { playToggle.invoke() },
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
        ) {
            if (player.isPlaying) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    imageVector = Icons.Filled.PauseCircleFilled,
                    contentDescription = stringResource(R.string.pause_video),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            } else {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    imageVector = Icons.Filled.PlayCircleFilled,
                    contentDescription = stringResource(R.string.play_video),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
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
    maxScale: Float = 10f,
    isRotation: Boolean = false,
    onItemClick: () -> Unit
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
                onClick = {
                    onItemClick()
                },
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
                awaitEachGesture {
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
            contentDescription = media.label
        ) { request ->
            request.centerCrop().dontTransform()
        }
    }
}

@Composable
fun BoxScope.VideoDurationHeader(modifier: Modifier = Modifier, media: Media) {
    Row(
        modifier = modifier
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

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MediaImage(
    media: Media,
    preloadRequestBuilder: RequestBuilder<Drawable>,
    selectionState: MutableState<Boolean>,
    isSelected: Boolean
) {
    val selectedSize = if (isSelected) 12.dp else 0.dp
    val scale = if (isSelected) 0.5f else 1f
    val selectedShape = if (isSelected) Shapes.large else Shapes.extraSmall
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .size(Dimens.Photo())
    ) {
        GlideImage(
            modifier = Modifier
                .fillMaxSize()
                .padding(selectedSize)
                .clip(selectedShape),
            model = File(media.path),
            contentDescription = media.label,
            contentScale = ContentScale.Crop,
        ) {
            it.thumbnail(preloadRequestBuilder)
                .signature(MediaStoreSignature(media.mimeType, media.timestamp, media.orientation))
                .priority(Priority.HIGH)
        }
        if (media.duration != null) {
            VideoDurationHeader(
                modifier = Modifier
                    .padding(selectedSize / 2)
                    .scale(scale),
                media = media
            )
        }
        if (media.favorite == 1) {
            Image(
                modifier = Modifier
                    .padding(selectedSize / 2)
                    .scale(scale)
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(16.dp),
                imageVector = Icons.Filled.Favorite,
                colorFilter = ColorFilter.tint(Color.Red),
                contentDescription = null
            )
        }

        AnimatedVisibility(
            visible = selectionState.value,
            enter = Animation.enterAnimation,
            exit = Animation.exitAnimation
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Black40P, Color.Transparent)))
            ) {
                RadioButton(
                    modifier = Modifier.padding(8.dp),
                    selected = isSelected,
                    onClick = null
                )
            }
        }
    }
}
