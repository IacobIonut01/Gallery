/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.albums.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.dot.gallery.R
import com.dot.gallery.core.Settings.Album.rememberAlbumSize
import com.dot.gallery.core.presentation.components.util.AutoResizeText
import com.dot.gallery.core.presentation.components.util.FontSizeRange
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.presentation.util.vibrate
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
        AutoResizeText(
            modifier = Modifier
                .padding(top = 12.dp)
                .padding(horizontal = 16.dp),
            text = album.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontSizeRange = FontSizeRange(
                min = 10.sp,
                max = 16.sp
            )
        )
        AutoResizeText(
            modifier = Modifier
                .padding(top = 2.dp, bottom = 16.dp)
                .padding(horizontal = 16.dp),
            text = pluralStringResource(id = R.plurals.item_count, count = album.count.toInt(), album.count),
            style = MaterialTheme.typography.labelMedium,
            fontSizeRange = FontSizeRange(
                min = 6.sp,
                max = 12.sp
            )
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun AlbumImage(
    album: Album,
    onItemClick: (Album) -> Unit,
    onItemLongClick: (Album) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()
    val radius = if (isPressed.value) 32.dp else 16.dp
    val cornerRadius by animateDpAsState(targetValue = radius, label = "cornerRadius")
    val view = LocalView.current
    val albumSize by rememberAlbumSize()
    GlideImage(
        modifier = Modifier
            .aspectRatio(1f)
            .size(Dp(albumSize))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(cornerRadius)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = rememberRipple(),
                onClick = { onItemClick(album) },
                onLongClick = {
                    view.vibrate()
                    onItemLongClick(album)
                }
            ),
        model = File(album.pathToThumbnail),
        contentDescription = album.label,
        contentScale = ContentScale.Crop,
    )
}