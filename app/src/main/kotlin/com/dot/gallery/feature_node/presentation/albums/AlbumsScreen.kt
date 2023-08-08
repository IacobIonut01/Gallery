/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.albums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.Settings.Album.rememberAlbumSize
import com.dot.gallery.core.Settings.Album.rememberLastSort
import com.dot.gallery.core.presentation.components.EmptyMedia
import com.dot.gallery.core.presentation.components.Error
import com.dot.gallery.core.presentation.components.FilterButton
import com.dot.gallery.feature_node.presentation.albums.components.AlbumComponent
import com.dot.gallery.feature_node.presentation.albums.components.CarouselPinnedAlbums
import com.dot.gallery.feature_node.presentation.search.MainSearchBar
import com.dot.gallery.feature_node.presentation.util.Screen

@Composable
fun AlbumsScreen(
    navigate: (route: String) -> Unit,
    toggleNavbar: (Boolean) -> Unit,
    paddingValues: PaddingValues,
    viewModel: AlbumsViewModel = hiltViewModel(),
    isScrolling: MutableState<Boolean>,
) {
    val state by viewModel.albumsState.collectAsStateWithLifecycle()
    val pinnedState by viewModel.pinnedAlbumState.collectAsStateWithLifecycle()
    val filterOptions = viewModel.rememberFilters()
    val albumSortSetting by rememberLastSort()
    val albumSize by rememberAlbumSize()
    val gridState = rememberLazyGridState()

    LaunchedEffect(state.albums, albumSortSetting) {
        val filterOption = filterOptions.first { it.selected }
        filterOption.onClick(filterOption.mediaOrder)
    }
    LaunchedEffect(gridState.isScrollInProgress) {
        isScrolling.value = gridState.isScrollInProgress
    }

    Scaffold(
        topBar = {
            MainSearchBar(
                bottomPadding = paddingValues.calculateBottomPadding(),
                navigate = navigate,
                toggleNavbar = toggleNavbar,
                isScrolling = isScrolling
            ) {
                var expandedDropdown by remember { mutableStateOf(false) }
                IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.drop_down_cd)
                    )
                }
                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.settings_title)) },
                        onClick = {
                            expandedDropdown = false
                            navigate(Screen.SettingsScreen.route)
                        }
                    )
                }
            }
        }
    ) {
        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxSize(),
            columns = GridCells.Adaptive(Dp(albumSize)),
            contentPadding = PaddingValues(
                top = it.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 16.dp + 64.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            navigate(Screen.TrashedScreen.route)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(id = R.string.trash)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(id = R.string.trash)
                        )
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            navigate(Screen.FavoriteScreen.route)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(id = R.string.favorites)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(id = R.string.favorites)
                        )
                    }
                }
            }
            if (pinnedState.albums.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .padding(bottom = 24.dp),
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
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FilterButton(filterOptions = filterOptions.toTypedArray())
                }
            }
            items(
                items = state.albums,
                key = { item -> item.toString() }
            ) { item ->
                AlbumComponent(
                    album = item,
                    onItemClick = viewModel.onAlbumClick(navigate),
                    onTogglePinClick = viewModel.onAlbumLongClick
                )
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