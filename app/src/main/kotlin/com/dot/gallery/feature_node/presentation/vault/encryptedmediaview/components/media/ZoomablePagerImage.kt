/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.vault.encryptedmediaview.components.media

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION
import com.dot.gallery.core.Settings.Misc.rememberAllowBlur
import com.dot.gallery.core.presentation.components.util.LocalBatteryStatus
import com.dot.gallery.core.presentation.components.util.ProvideBatteryStatus
import com.dot.gallery.core.presentation.components.util.swipe
import com.dot.gallery.feature_node.domain.model.DecryptedMedia
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.rememberAsyncImagePainter
import com.github.panpf.sketch.request.ComposableImageRequest
import com.github.panpf.zoomimage.ZoomImage

@Composable
fun ZoomablePagerImage(
    modifier: Modifier = Modifier,
    media: DecryptedMedia,
    uiEnabled: Boolean,
    onItemClick: () -> Unit,
    onSwipeDown: () -> Unit
) {
    val painter = rememberAsyncImagePainter(
        request = ComposableImageRequest(media.uri) {
            memoryCachePolicy(CachePolicy.ENABLED)
            crossfade()
            setExtra(
                key = "encryptedMediaKey",
                value = media.toString(),
            )
            setExtra("realMimeType", media.mimeType)
        },
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ProvideBatteryStatus {
            val allowBlur by rememberAllowBlur()
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

        ZoomImage(
            modifier = modifier
                .fillMaxSize()
                .swipe(
                    onSwipeDown = onSwipeDown
                ),
            onTap = { onItemClick() },
            painter = painter,
            contentScale = ContentScale.Fit,
            contentDescription = media.label
        )
    }


}
