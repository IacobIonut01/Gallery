/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.ignored.ui.setup.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.LocalMediaImageRenderer
import com.dot.gallery.core.ui.components.rememberSettingsFocusState
import com.dot.gallery.core.ui.components.settingsFocusTarget
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.uri.emptyUri
import com.dot.gallery.feature.ignored.ui.IgnoredPreviewHost
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.dot.gallery.feature.ignored.generated.resources.Res
import com.dot.gallery.feature.ignored.generated.resources.album_items_count


/**
 * A selectable album grid item with thumbnail, selection indicator,
 * and label. Supports disabled state for already-ignored albums.
 */
@Composable
fun SelectableAlbumItem(
    album: Album,
    isSelected: Boolean,
    isDisabled: Boolean,
    showCheckmark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberSettingsFocusState()
    val alpha by animateFloatAsState(
        targetValue = if (isDisabled) 0.4f else 1f,
        label = "alpha"
    )

    val focusBorderWidth by animateDpAsState(
        targetValue = if (focusState.hasFocus) 3.dp else 0.dp,
        label = "albumFocusBorderWidth"
    )
    val focusBorderColor by animateColorAsState(
        targetValue = if (focusState.hasFocus) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
        label = "albumFocusBorderColor"
    )

    Box(
        modifier = modifier
            .settingsFocusTarget(focusState)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(focusBorderWidth, focusBorderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = !isDisabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                LocalMediaImageRenderer.current.RenderImage(
                    model = album.uri,
                    contentDescription = album.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .alpha(alpha),
                    signature = album
                )

                // Selection indicator
                if (showCheckmark && isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(24.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = album.label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp).padding(horizontal = 8.dp)
            )

            Text(
                text = stringResource(Res.string.album_items_count, album.count),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp)
            )
        }
    }
}

// ========== Previews ==========

@Preview
@Composable
private fun SelectableAlbumItemUnselectedPreview() {
    IgnoredPreviewHost {
        SelectableAlbumItem(
            album = Album(
                id = 1,
                label = "Camera",
                uri = emptyUri,
                pathToThumbnail = "/DCIM/Camera/photo.jpg",
                relativePath = "DCIM/Camera",
                timestamp = 0,
                count = 42
            ),
            isSelected = false,
            isDisabled = false,
            showCheckmark = true,
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun SelectableAlbumItemSelectedPreview() {
    IgnoredPreviewHost {
        SelectableAlbumItem(
            album = Album(
                id = 2,
                label = "Screenshots",
                uri = emptyUri,
                pathToThumbnail = "/Pictures/Screenshots/shot.png",
                relativePath = "Pictures/Screenshots",
                timestamp = 0,
                count = 156
            ),
            isSelected = true,
            isDisabled = false,
            showCheckmark = true,
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun SelectableAlbumItemDisabledPreview() {
    IgnoredPreviewHost {
        SelectableAlbumItem(
            album = Album(
                id = 3,
                label = "Already Hidden",
                uri = emptyUri,
                pathToThumbnail = "/Pictures/Hidden/img.jpg",
                relativePath = "Pictures/Hidden",
                timestamp = 0,
                count = 10
            ),
            isSelected = false,
            isDisabled = true,
            showCheckmark = true,
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun SelectableAlbumItemLongLabelPreview() {
    IgnoredPreviewHost {
        SelectableAlbumItem(
            album = Album(
                id = 4,
                label = "Very Long Album Name That Gets Truncated",
                uri = emptyUri,
                pathToThumbnail = "/Pictures/LongName/img.jpg",
                relativePath = "Pictures/LongName",
                timestamp = 0,
                count = 99
            ),
            isSelected = false,
            isDisabled = false,
            showCheckmark = true,
            onClick = {}
        )
    }
}
