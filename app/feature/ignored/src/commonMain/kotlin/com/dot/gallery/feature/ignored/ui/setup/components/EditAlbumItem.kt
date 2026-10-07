/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.ignored.ui.setup.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.LocalMediaImageRenderer
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.uri.emptyUri
import com.dot.gallery.feature.ignored.ui.IgnoredPreviewHost
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock


@Composable
fun EditAlbumItem(
    album: Album,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else Modifier
                )
                .clickable(onClick = onClick)
                .fillMaxSize(),
        ) {
            LocalMediaImageRenderer.current.RenderImage(
                modifier = Modifier.fillMaxSize(),
                model = album.uri,
                contentDescription = album.label,
                contentScale = ContentScale.Crop,
                signature = album
            )
            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        )
                )
            }
        }

        Text(
            text = album.label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }

}

private val previewAlbum = Album(
    id = 1,
    label = "Camera",
    uri = emptyUri,
    pathToThumbnail = "",
    relativePath = "DCIM/Camera",
    timestamp = Clock.System.now().toEpochMilliseconds(),
    count = 100
)

@Preview
@Composable
private fun EditAlbumItemUnselectedPreview() {
    IgnoredPreviewHost {
        EditAlbumItem(
            album = previewAlbum,
            isSelected = false,
            onClick = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview
@Composable
private fun EditAlbumItemSelectedPreview() {
    IgnoredPreviewHost {
        EditAlbumItem(
            album = previewAlbum,
            isSelected = true,
            onClick = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview
@Composable
private fun EditAlbumItemLongLabelPreview() {
    IgnoredPreviewHost {
        EditAlbumItem(
            album = previewAlbum.copy(label = "Very Long Album Name That Should Be Truncated"),
            isSelected = false,
            onClick = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}
