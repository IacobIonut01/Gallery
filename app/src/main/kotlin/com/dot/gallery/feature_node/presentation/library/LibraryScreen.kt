package com.dot.gallery.feature_node.presentation.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokar.pinchzoomgrid.PinchZoomGridLayout
import com.dokar.pinchzoomgrid.rememberPinchZoomGridState
import com.dot.gallery.R
import com.dot.gallery.core.Constants.albumCellsList
import com.dot.gallery.core.Settings.Album.rememberAlbumGridSize
import com.dot.gallery.feature_node.presentation.library.components.LibrarySmallItem
import com.dot.gallery.feature_node.presentation.search.MainSearchBar
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.ui.core.icons.Encrypted
import com.dot.gallery.ui.core.Icons as GalleryIcons

@Composable
fun LibraryScreen(
    navigate: (route: String) -> Unit,
    toggleNavbar: (Boolean) -> Unit,
    paddingValues: PaddingValues,
    isScrolling: MutableState<Boolean>,
    searchBarActive: MutableState<Boolean>
) {
    val viewModel = hiltViewModel<LibraryViewModel>()
    var lastCellIndex by rememberAlbumGridSize()

    val pinchState = rememberPinchZoomGridState(
        cellsList = albumCellsList,
        initialCellsIndex = lastCellIndex
    )

    LaunchedEffect(pinchState.isZooming) {
        lastCellIndex = albumCellsList.indexOf(pinchState.currentCells)
    }

    val indicatorState by viewModel.indicatorState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            MainSearchBar(
                bottomPadding = paddingValues.calculateBottomPadding(),
                navigate = navigate,
                toggleNavbar = toggleNavbar,
                isScrolling = isScrolling,
                activeState = searchBarActive
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
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "headerButtons"
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pinchItem(key = "headerButtons")
                            .padding(horizontal = 16.dp)
                            .padding(top = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                16.dp,
                                Alignment.CenterHorizontally
                            )
                        ) {
                            LibrarySmallItem(
                                title = stringResource(R.string.trash),
                                icon = Icons.Outlined.DeleteOutline,
                                contentColor = MaterialTheme.colorScheme.primary,
                                useIndicator = true,
                                indicatorCounter = indicatorState.trashCount,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        navigate(Screen.TrashedScreen.route)
                                    }
                            )
                            LibrarySmallItem(
                                title = stringResource(R.string.favorites),
                                icon = Icons.Outlined.FavoriteBorder,
                                contentColor = MaterialTheme.colorScheme.error,
                                useIndicator = true,
                                indicatorCounter = indicatorState.favoriteCount,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        navigate(Screen.FavoriteScreen.route)
                                    }
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                16.dp,
                                Alignment.CenterHorizontally
                            )
                        ) {
                            LibrarySmallItem(
                                title = stringResource(R.string.vault),
                                icon = GalleryIcons.Encrypted,
                                contentColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        navigate(Screen.VaultScreen())
                                    },
                                contentDescription = stringResource(R.string.vault)
                            )
                            LibrarySmallItem(
                                title = stringResource(R.string.ignored),
                                icon = Icons.Outlined.VisibilityOff,
                                contentColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        navigate(Screen.IgnoredScreen())
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}