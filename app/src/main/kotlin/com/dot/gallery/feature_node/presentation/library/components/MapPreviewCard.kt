/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.library.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.dot.gallery.core.Settings
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.presentation.util.GlideInvalidation
import com.dot.gallery.feature_node.presentation.util.StaticMapURL

/**
 * A card showing a static map preview with a circular photo thumbnail,
 * used in the Library screen to replace the plain "Locations" header.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MapPreviewCard(
    modifier: Modifier = Modifier,
    latestMedia: Media.UriMedia?,
    latitude: Double?,
    longitude: Double?,
    effectiveAppIsDark: Boolean,
) {
    val mapAppearance by Settings.Misc.rememberMapAppearance()
    val mapTileUrl = remember(latitude, longitude, mapAppearance, effectiveAppIsDark) {
        StaticMapURL(
            latitude = latitude ?: 46.77,
            longitude = longitude ?: 23.59,
            appearance = mapAppearance,
            effectiveAppIsDark = effectiveAppIsDark,
            zoom = 8,
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        // Map tile background
        GlideImage(
            model = mapTileUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            requestBuilderTransform = {
                it.diskCacheStrategy(DiskCacheStrategy.ALL)
            }
        )

        // Circular photo thumbnail at center
        if (latestMedia != null) {
            GlideImage(
                model = latestMedia.getUri(),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .align(Alignment.Center),
                contentScale = ContentScale.Crop,
                requestBuilderTransform = {
                    it.signature(GlideInvalidation.signature(latestMedia))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                }
            )
        }
    }
}
