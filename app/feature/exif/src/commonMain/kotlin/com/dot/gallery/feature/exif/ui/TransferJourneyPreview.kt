/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.exif.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.core.presentation.components.LocalMediaImageRenderer
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.getUri
import com.dot.gallery.feature.albums.ui.components.ProviderIconSlot
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.exif.generated.resources.Res
import com.dot.gallery.feature.exif.generated.resources.category_media_count
import com.dot.gallery.feature.exif.generated.resources.transfer_copying
import com.dot.gallery.feature.exif.generated.resources.transfer_moving

@Composable
fun TransferJourneyPreview(
    media: List<Media>,
    destination: Album?,
    destinationLabel: String,
    mode: TransferSheetMode,
    progress: Float,
    modifier: Modifier = Modifier,
    showProgress: Boolean = true,
    showStatus: Boolean = true,
    providerIcon: ProviderIconSlot? = null
) {
    val animatedProgress by animateFloatAsState(progress.coerceIn(0f, 1f), label = "transferProgress")
    val sourceLabel = media.singleOrNull()?.label
        ?: stringResource(Res.string.category_media_count, media.size)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TransferThumbnail(
                    model = media.firstOrNull()?.let { runCatching { it.getUri() }.getOrNull() },
                    label = sourceLabel,
                    count = media.size.takeIf { it > 1 },
                    fallbackProvider = null,
                    providerIcon = providerIcon
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                TransferThumbnail(
                    model = destination?.uri,
                    label = destinationLabel,
                    count = null,
                    fallbackProvider = destination?.cloudIdentity?.providerType,
                    providerIcon = providerIcon
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = sourceLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = destinationLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showProgress) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
            if (showStatus) {
                Text(
                    text = stringResource(
                        if (mode == TransferSheetMode.MOVE) Res.string.transfer_moving
                        else Res.string.transfer_copying
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TransferThumbnail(
    model: Any?,
    label: String,
    count: Int?,
    fallbackProvider: ProviderType?,
    providerIcon: ProviderIconSlot?
) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (model != null && model.toString().isNotBlank()) {
            LocalMediaImageRenderer.current.RenderImage(
                modifier = Modifier.matchParentSize(),
                model = model,
                contentScale = ContentScale.Crop,
                contentDescription = label,
                signature = model
            )
        } else if (fallbackProvider != null && providerIcon != null) {
            providerIcon(fallbackProvider, MaterialTheme.colorScheme.primary, Modifier.size(40.dp))
        } else if (fallbackProvider != null) {
            Icon(
                imageVector = Icons.Outlined.Cloud,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (count != null) {
            Text(
                text = count.toString(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
