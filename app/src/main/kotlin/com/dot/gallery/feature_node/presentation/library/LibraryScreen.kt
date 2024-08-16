package com.dot.gallery.feature_node.presentation.library

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.unit.dp
import com.dokar.pinchzoomgrid.PinchZoomGridLayout
import com.dokar.pinchzoomgrid.rememberPinchZoomGridState
import com.dot.gallery.R
import com.dot.gallery.core.Constants.albumCellsList
import com.dot.gallery.core.Settings.Album.rememberAlbumGridSize
import com.dot.gallery.feature_node.presentation.common.MediaViewModel
import com.dot.gallery.feature_node.presentation.search.MainSearchBar
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.ui.core.icons.Encrypted

@Composable
fun LibraryScreen(
    navigate: (route: String) -> Unit,
    mediaViewModel: MediaViewModel,
    toggleNavbar: (Boolean) -> Unit,
    paddingValues: PaddingValues,
    isScrolling: MutableState<Boolean>,
    searchBarActive: MutableState<Boolean>
) {
    var lastCellIndex by rememberAlbumGridSize()

    val pinchState = rememberPinchZoomGridState(
        cellsList = albumCellsList,
        initialCellsIndex = lastCellIndex
    )

    LaunchedEffect(pinchState.isZooming) {
        lastCellIndex = albumCellsList.indexOf(pinchState.currentCells)
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
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    navigate(Screen.VaultScreen())
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(),
                                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            ) {
                                Icon(
                                    imageVector = com.dot.gallery.ui.core.Icons.Encrypted,
                                    contentDescription = stringResource(id = R.string.vault)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = stringResource(R.string.vault)
                                )
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    navigate(Screen.IgnoredScreen())
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VisibilityOff,
                                    contentDescription = stringResource(R.string.ignored_albums)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = stringResource(id = R.string.ignored_albums)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}