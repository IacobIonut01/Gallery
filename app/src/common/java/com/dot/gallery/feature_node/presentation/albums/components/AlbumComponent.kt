/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.albums.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dot.gallery.R
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.ui.theme.Dimens
import com.dot.gallery.ui.theme.Shapes
import java.io.File

@Composable
fun AlbumComponent(
    album: Album,
    onItemClick: (Album) -> Unit,
    onTogglePinClick: (Album) -> Unit
) {
    val showDropDown = remember { mutableStateOf(false) }
    val pinTitle =
        if (album.isPinned) stringResource(R.string.unpin) else stringResource(R.string.pin)
    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        AlbumImage(album = album, onItemClick) {
            showDropDown.value = !showDropDown.value
        }
        Text(
            modifier = Modifier
                .padding(top = 12.dp)
                .padding(horizontal = 16.dp),
            text = album.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            modifier = Modifier
                .padding(top = 2.dp, bottom = 16.dp)
                .padding(horizontal = 16.dp),
            text = pluralStringResource(id = R.plurals.item_count, count = album.count.toInt(), album.count),
            style = MaterialTheme.typography.labelMedium
        )
        DropdownMenu(
            expanded = showDropDown.value,
            offset = DpOffset(16.dp, (-64).dp),
            onDismissRequest = { showDropDown.value = false }) {
            DropdownMenuItem(
                text = { Text(text = pinTitle) },
                onClick = {
                    onTogglePinClick(album)
                    showDropDown.value = false
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumImage(
    album: Album,
    onItemClick: (Album) -> Unit,
    onItemLongClick: (Album) -> Unit
) {
    AsyncImage(
        modifier = Modifier
            .aspectRatio(1f)
            .size(Dimens.Album())
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = Shapes.large
            )
            .clip(Shapes.large)
            .combinedClickable(
                onClick = { onItemClick(album) },
                onLongClick = { onItemLongClick(album) }
            ),
        model = File(album.pathToThumbnail),
        contentDescription = album.label,
        contentScale = ContentScale.Crop,
    )
}