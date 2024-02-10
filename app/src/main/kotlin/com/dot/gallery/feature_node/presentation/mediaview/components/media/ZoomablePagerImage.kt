/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components.media

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Scale
import com.dot.gallery.core.Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION
import com.dot.gallery.core.Settings
import com.dot.gallery.core.presentation.components.util.LocalBatteryStatus
import com.dot.gallery.core.presentation.components.util.ProvideBatteryStatus
import com.dot.gallery.feature_node.domain.model.Media
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableImage
import me.saket.telephoto.zoomable.ZoomableImageSource
import me.saket.telephoto.zoomable.coil3.coil3
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import net.engawapg.lib.zoomable.rememberZoomState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZoomablePagerImage(
    modifier: Modifier = Modifier,
    media: Media,
    uiEnabled: Boolean,
    maxScale: Float = 10f,
    onItemClick: () -> Unit
) {
    val zoomState = rememberZoomState(
        maxScale = maxScale,
    )
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(media.uri)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .placeholderMemoryCacheKey(media.toString())
            .scale(Scale.FILL)
            .build(),
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        onSuccess = {
            zoomState.setContentSize(it.painter.intrinsicSize)
        }
    )
    val zoomableState = rememberZoomableState(
        zoomSpec = ZoomSpec(
            maxZoomFactor = maxScale
        )
    )
    val state = rememberZoomableImageState(
        zoomableState = zoomableState
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ProvideBatteryStatus {
            val allowBlur by Settings.Misc.rememberAllowBlur()
            val isPowerSavingMode = LocalBatteryStatus.current.isPowerSavingMode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && allowBlur && !isPowerSavingMode) {
                val blurAlpha by animateFloatAsState(
                    animationSpec = tween(DEFAULT_TOP_BAR_ANIMATION_DURATION),
                    targetValue = if (uiEnabled) 0.7f else 0f,
                    label = "blurAlpha"
                )
                Image(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(blurAlpha)
                        .blur(100.dp),
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
        }

        ZoomableImage(
            modifier = modifier
                .fillMaxSize()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onDoubleClick = {},
                    onClick = onItemClick
                ),
            state = state,
            image = ZoomableImageSource.coil3(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(media.uri)
                    .scale(Scale.FILL)
                    .placeholderMemoryCacheKey(media.toString())
                    .build()
            ),
            contentScale = ContentScale.Fit,
            contentDescription = media.label
        )
    }


}
