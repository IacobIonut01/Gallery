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
import com.dot.gallery.core.Settings
import com.dot.gallery.core.presentation.components.util.LocalBatteryStatus
import com.dot.gallery.core.presentation.components.util.ProvideBatteryStatus
import com.dot.gallery.feature_node.domain.model.EncryptedMedia
import com.github.panpf.sketch.fetch.newBase64Uri
import com.github.panpf.sketch.request.ComposableImageRequest
import com.github.panpf.zoomimage.SketchZoomAsyncImage

@Composable
fun ZoomablePagerImage(
    modifier: Modifier = Modifier,
    media: EncryptedMedia,
    uiEnabled: Boolean,
    onItemClick: () -> Unit
) {
    val painter = com.github.panpf.sketch.rememberAsyncImagePainter(
        request = ComposableImageRequest(newBase64Uri(mimeType = media.mimeType, imageData = media.bytes)) {
            memoryCachePolicy(com.github.panpf.sketch.cache.CachePolicy.ENABLED)
            crossfade()
        },
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
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

        SketchZoomAsyncImage(
            modifier = modifier.fillMaxSize(),
            onTap = { onItemClick() },
            uri = newBase64Uri(mimeType = media.mimeType, imageData = media.bytes),
            contentScale = ContentScale.Fit,
            contentDescription = media.label
        )
    }


}
