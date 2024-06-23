/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.albums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokar.pinchzoomgrid.PinchZoomGridLayout
import com.dokar.pinchzoomgrid.rememberPinchZoomGridState
import com.dot.gallery.R
import com.dot.gallery.core.Constants.albumCellsList
import com.dot.gallery.core.Settings.Album.rememberAlbumGridSize
import com.dot.gallery.core.Settings.Album.rememberLastSort
import com.dot.gallery.core.presentation.components.EmptyMedia
import com.dot.gallery.core.presentation.components.Error
import com.dot.gallery.core.presentation.components.FilterButton
import com.dot.gallery.feature_node.presentation.albums.components.AlbumComponent
import com.dot.gallery.feature_node.presentation.albums.components.CarouselPinnedAlbums
import com.dot.gallery.feature_node.presentation.common.MediaViewModel
import com.dot.gallery.feature_node.presentation.search.MainSearchBar
import com.dot.gallery.feature_node.presentation.util.Screen

@Composable
fun AlbumsScreen(
    navigate: (route: String) -> Unit,
    mediaViewModel: MediaViewModel,
    toggleNavbar: (Boolean) -> Unit,
    paddingValues: PaddingValues,
    viewModel: AlbumsViewModel,
    isScrolling: MutableState<Boolean>,
    searchBarActive: MutableState<Boolean>
) {
    val state by viewModel.unPinnedAlbumsState.collectAsStateWithLifecycle()
    val pinnedState by viewModel.pinnedAlbumState.collectAsStateWithLifecycle()
    val filterOptions = viewModel.rememberFilters()
    val albumSortSetting by rememberLastSort()
    var lastCellIndex by rememberAlbumGridSize()

    val pinchState = rememberPinchZoomGridState(
        cellsList = albumCellsList,
        initialCellsIndex = lastCellIndex
    )

    LaunchedEffect(pinchState.isZooming) {
        lastCellIndex = albumCellsList.indexOf(pinchState.currentCells)
    }

    LaunchedEffect(state.albums, albumSortSetting) {
        val filterOption = filterOptions.first { it.selected }
        filterOption.onClick(filterOption.mediaOrder)
    }

    Scaffold(
        topBar = {
            MainSearchBar(
                mediaViewModel = mediaViewModel,
                bottomPadding = paddingValues.calculateBottomPadding(),
                navigate = navigate,
                toggleNavbar = toggleNavbar,
                isScrolling = isScrolling,
                activeState = searchBarActive
            ) {
                IconButton(onClick = { navigate(Screen.SettingsScreen.route) }) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings_title)
                    )
                }
            }
        }
    ) {
        PinchZoomGridLayout(state = pinchState) {
            LaunchedEffect(gridState.isScrollInProgress) {
                isScrolling.value = gridState.isScrollInProgress
            }
            LazyVerticalGrid(
                state = gridState,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxSize(),
                columns = gridCells,
                contentPadding = PaddingValues(
                    top = it.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + 16.dp + 64.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pinnedState.albums.isNotEmpty()) {
                    item(
                        span = { GridItemSpan(maxLineSpan) },
                        key = "pinnedAlbums"
                    ) {
                        Column {
                            Text(
                                modifier = Modifier
                                    .pinchItem(key = "pinnedAlbums")
                                    .padding(horizontal = 8.dp)
                                    .padding(vertical = 24.dp),
                                text = stringResource(R.string.pinned_albums_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            CarouselPinnedAlbums(
                                albumList = pinnedState.albums,
                                onAlbumClick = viewModel.onAlbumClick(navigate),
                                onAlbumLongClick = viewModel.onAlbumLongClick
                            )
                        }
                    }
                }
                if (state.albums.isNotEmpty()) {
                    item(
                        span = { GridItemSpan(maxLineSpan) },
                        key = "filterButton"
                    ) {
                        FilterButton(
                            modifier = Modifier.pinchItem(key = "filterButton"),
                            filterOptions = filterOptions.toTypedArray()
                        )
                    }
                }
                items(
                    items = state.albums,
                    key = { item -> item.toString() }
                ) { item ->
                    AlbumComponent(
                        modifier = Modifier.pinchItem(key = item.toString()),
                        album = item,
                        onItemClick = viewModel.onAlbumClick(navigate),
                        onTogglePinClick = viewModel.onAlbumLongClick
                    )
                }
            }
        }
        /** Error State Handling Block **/
        if (state.error.isNotEmpty()) {
            Error(errorMessage = state.error)
        } else if (state.albums.isEmpty() && pinnedState.albums.isEmpty()) {
            EmptyMedia(modifier = Modifier.fillMaxSize())
        }
        /** ************ **/
    }
}