/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.classifier.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.LocalMediaImageRenderer
import com.dot.gallery.core.ui.settings.rememberPreference
import com.dot.gallery.domain.model.LocationMedia
import com.dot.gallery.domain.model.locationIdentityKey
import com.dot.gallery.domain.settings.SettingsKeys
import com.dot.gallery.domain.util.getUri
import com.dot.gallery.ui.theme.BlackScrim
import com.dot.gallery.ui.theme.WhiterBlackScrim
import com.dot.gallery.ui.theme.isDarkTheme
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.classifier.generated.resources.Res
import com.dot.gallery.feature.classifier.generated.resources.locations


/**
 * A horizontal carousel of location recommendations for the search screen.
 * Matches the library screen location carousel style.
 */
@Composable
fun LocationCarousel(
    locations: List<LocationMedia>,
    onLocationClick: (LocationMedia) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = stringResource(Res.string.locations),
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp)
) {
    if (locations.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(contentPadding)
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = locations,
                key = { it.locationIdentityKey() }
            ) { locationMedia ->
                val imageRenderer = LocalMediaImageRenderer.current
                val isDarkTheme = isDarkTheme()
                val allowBlur by rememberPreference(SettingsKeys.allowBlur)
                val followTheme = remember(allowBlur) { !allowBlur }
                val gradientColor by animateColorAsState(
                    if (followTheme) {
                        if (isDarkTheme) BlackScrim else WhiterBlackScrim
                    } else BlackScrim,
                )
                Box(
                    modifier = Modifier
                        .width(164.dp)
                        .height(256.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onLocationClick(locationMedia) },
                ) {
                    imageRenderer.RenderImage(
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        model = locationMedia.media.getUri(),
                        contentDescription = locationMedia.location,
                        signature = locationMedia.media
                    )
                    Text(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        gradientColor
                                    )
                                )
                            )
                            .padding(24.dp),
                        text = locationMedia.location,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
