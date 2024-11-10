/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components.media

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION
import com.dot.gallery.core.Settings
import com.dot.gallery.core.presentation.components.util.LocalBatteryStatus
import com.dot.gallery.core.presentation.components.util.ProvideBatteryStatus
import com.dot.gallery.core.presentation.components.util.swipe
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.util.rememberFeedbackManager
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.rememberAsyncImageState
import com.github.panpf.sketch.request.ComposableImageRequest
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.util.Size
import com.github.panpf.zoomimage.SketchZoomAsyncImage
import com.github.panpf.zoomimage.rememberSketchZoomState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Stable
@NonRestartableComposable
@Composable
fun ZoomablePagerImage(
    modifier: Modifier = Modifier,
    media: Media,
    uiEnabled: Boolean,
    onItemClick: () -> Unit,
    onSwipeDown: () -> Unit
) {
    val feedbackManager = rememberFeedbackManager()
    var isRotating by rememberSaveable { mutableStateOf(false) }
    var currentRotation by rememberSaveable { mutableIntStateOf(0) }
    val rotationAnimation by animateFloatAsState(
        targetValue = if (isRotating) 90f else 0f,
        label = "rotationAnimation"
    )
    ProvideBatteryStatus {
        val allowBlur by Settings.Misc.rememberAllowBlur()
        val isPowerSavingMode = LocalBatteryStatus.current.isPowerSavingMode
        AnimatedVisibility(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && allowBlur && !isPowerSavingMode) {
            val blurAlpha by animateFloatAsState(
                animationSpec = tween(DEFAULT_TOP_BAR_ANIMATION_DURATION),
                targetValue = if (uiEnabled) 0.7f else 0f,
                label = "blurAlpha"
            )
            AsyncImage(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(blurAlpha)
                    .blur(100.dp),
                request = ComposableImageRequest(media.uri.toString()) {
                    size(Size.parseSize("600x600"))
                    setExtra(
                        key = "mediaKey",
                        value = media.toString(),
                    )
                },
                contentDescription = null,
                filterQuality = FilterQuality.None,
                contentScale = ContentScale.Crop
            )
        }
    }
    val zoomState = rememberSketchZoomState()
    val scope = rememberCoroutineScope()
    val asyncState = rememberAsyncImageState(
        options = ImageOptions {
            setExtra(
                key = "mediaKey",
                value = media.toString(),
            )
        }
    )

    SketchZoomAsyncImage(
        zoomState = zoomState,
        state = asyncState,
        modifier = modifier
            .fillMaxSize()
            .swipe(
                onSwipeDown = onSwipeDown
            )
            .graphicsLayer {
                rotationZ = if (isRotating) rotationAnimation else 0f
            },
        onTap = { onItemClick() },
        onLongPress = {
            scope.launch {
                isRotating = true
                feedbackManager.vibrate()
                currentRotation += 90
                delay(350)
                zoomState.zoomable.rotate(currentRotation)
                isRotating = false
            }
        },
        alignment = Alignment.Center,
        uri = media.uri.toString(),
        contentDescription = media.label
    )
}


