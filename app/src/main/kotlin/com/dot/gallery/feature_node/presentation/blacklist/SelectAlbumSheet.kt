package com.dot.gallery.feature_node.presentation.blacklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.Settings
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.BlacklistedAlbum
import com.dot.gallery.feature_node.presentation.albums.AlbumsViewModel
import com.dot.gallery.feature_node.presentation.albums.components.AlbumComponent
import com.dot.gallery.feature_node.presentation.util.AppBottomSheetState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAlbumSheet(
    sheetState: AppBottomSheetState,
    blacklistedAlbums: List<BlacklistedAlbum>,
    onSelect: (Album) -> Unit
) {
    val albumViewModel = hiltViewModel<AlbumsViewModel>()
    albumViewModel.attachToLifecycle()
    val state by albumViewModel.albumsState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    LaunchedEffect(blacklistedAlbums) {
        albumViewModel.getAlbums()
    }
    if (sheetState.isVisible) {
        ModalBottomSheet(
            sheetState = sheetState.sheetState,
            onDismissRequest = {
                scope.launch {
                    sheetState.hide()
                }
            },
            dragHandle = { DragHandle() },
            windowInsets = WindowInsets(
                0,
                WindowInsets.statusBars.getTop(LocalDensity.current),
                0,
                0
            )
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.select_an_album),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                )

                val albumSize by Settings.Album.rememberAlbumSize()
                LazyVerticalGrid(
                    state = rememberLazyGridState(),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    columns = GridCells.Adaptive(Dp(albumSize)),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        bottom = WindowInsets.navigationBars.getBottom(
                            LocalDensity.current
                        ).dp
                    )
                ) {
                    items(
                        items = state.albums,
                        key = { item -> item.toString() }
                    ) { item ->
                        AlbumComponent(
                            album = item,
                            onItemClick = { album ->
                                scope.launch {
                                    sheetState.hide()
                                    onSelect(album)
                                }
                            },
                            isEnabled = blacklistedAlbums.firstOrNull { it.id == item.id } == null,
                            onTogglePinClick = null
                        )
                    }
                }
            }
        }
    }
}