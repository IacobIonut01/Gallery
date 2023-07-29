/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.presentation.settings.customization.albumsize

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dot.gallery.R
import com.dot.gallery.core.Settings.Album.rememberAlbumSize
import com.dot.gallery.core.presentation.components.util.AutoResizeText
import com.dot.gallery.core.presentation.components.util.FontSizeRange
import com.dot.gallery.feature_node.domain.model.Album
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumSizeScreen(
    navigateUp: () -> Unit
) {
    val fakeAlbums = remember {
        listOf(
            Album(
                id = 1,
                label = "Camera",
                pathToThumbnail = "",
                timestamp = 10,
                count = 14
            ),
            Album(
                id = 2,
                label = "Screenshots",
                pathToThumbnail = "",
                timestamp = 9,
                count = 27
            ),
            Album(
                id = 3,
                label = "Pictures",
                pathToThumbnail = "",
                timestamp = 8,
                count = 10
            ),
            Album(
                id = 4,
                label = "My Album",
                pathToThumbnail = "",
                timestamp = 8,
                count = 104
            )
        )
    }
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Expanded,
        skipHiddenState = true
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)
    var albumSize by rememberAlbumSize()
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetPeekHeight = 64.dp,
        sheetContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.change_album_size),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(32.dp)
                ) {
                    OutlinedButton(
                        onClick = { albumSize = 182f },
                        border = null,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    ) {
                        Text(text = stringResource(R.string.reset))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val albumSizeStripped = remember(albumSize) { albumSize.toString().removeSuffix(".0") }
                    Text(
                        text = stringResource(R.string.size_dp, albumSizeStripped),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(100)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Slider(
                value = albumSize,
                onValueChange = { albumSize = it.roundToInt().toFloat() },
                valueRange = 60f..300f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 64.dp)
            )
        },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navigateUp() }) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(
                                id = R.string.back_cd
                            )
                        )
                    }
                }
            )
        },
        content = {
            LazyVerticalGrid(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxSize(),
                columns = GridCells.Adaptive(Dp(albumSize)),
                contentPadding = PaddingValues(
                    top = it.calculateTopPadding() + 16.dp,
                    bottom = it.calculateBottomPadding() + 16.dp + 64.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = fakeAlbums,
                    key = { item -> item.toString() }
                ) { item ->
                    FakeAlbumComponent(album = item)
                }
            }
        }
    )
}

@Composable
private fun FakeAlbumComponent(album: Album) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        FakeAlbumImage(album = album)
        AutoResizeText(
            modifier = Modifier
                .padding(top = 12.dp)
                .padding(horizontal = 16.dp),
            text = album.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            fontSizeRange = FontSizeRange(
                min = 8.sp,
                max = 16.sp
            )
        )
        AutoResizeText(
            modifier = Modifier
                .padding(top = 2.dp, bottom = 16.dp)
                .padding(horizontal = 16.dp),
            text = pluralStringResource(
                id = R.plurals.item_count,
                count = album.count.toInt(),
                album.count
            ),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            fontSizeRange = FontSizeRange(
                min = 4.sp,
                max = 12.sp
            )
        )
    }
}

@Composable
private fun FakeAlbumImage(album: Album) {
    val albumSize by rememberAlbumSize()
    val imageId = when(album.id) {
        1L -> R.drawable.image_sample_1
        2L -> R.drawable.image_sample_2
        3L -> R.drawable.image_sample_3
        4L -> R.drawable.image_sample_4
        else -> R.drawable.image_sample_1
    }
    Image(
        modifier = Modifier
            .aspectRatio(1f)
            .size(Dp(albumSize))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp)),
        painter = painterResource(id = imageId),
        contentDescription = album.label,
        contentScale = ContentScale.Crop,
    )
}