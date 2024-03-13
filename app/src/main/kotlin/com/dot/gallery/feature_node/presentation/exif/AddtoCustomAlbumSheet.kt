package com.dot.gallery.feature_node.presentation.exif

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.Constants.albumCellsList
import com.dot.gallery.core.Settings.Album.rememberAlbumGridSize
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.albums.CustomAlbumsViewModel
import com.dot.gallery.feature_node.presentation.albums.components.AlbumComponent
import com.dot.gallery.feature_node.presentation.customalbums.components.CustomAlbumComponent
import com.dot.gallery.feature_node.presentation.util.AppBottomSheetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddtoCustomAlbumSheet(
    sheetState: AppBottomSheetState,
    mediaList: List<Media>,
    customAlbumsViewModel: CustomAlbumsViewModel
) {

    val scope = rememberCoroutineScope()
    val customAlbumsState by customAlbumsViewModel.albumsState.collectAsStateWithLifecycle()

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
                    text = stringResource(R.string.add_to_custom_album),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                )


                val albumSize by rememberAlbumGridSize()
                LazyVerticalGrid(
                    state = rememberLazyGridState(),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    columns = albumCellsList[albumSize]
                ) {
                    item {
                        AlbumComponent(
                            album = Album.NewAlbum,
                            isEnabled = true,
                            onItemClick = {
                                scope.launch(Dispatchers.Main) {

                                }
                            }
                        )
                    }
                    items(
                        items = customAlbumsState.albums,
                        key = { item -> item.toString() }
                    ) { item ->
                        CustomAlbumComponent(
                            album = item,
                            onItemClick = {
                                scope.launch(Dispatchers.Main) {
                                    println("Add to Album ${it.label}")
                                    async {
                                        mediaList.forEach { media ->
                                            customAlbumsViewModel.addMediaToAlbum(it, media.id)
                                            println("--add media ${media.label}")
                                        }
                                    }.await()
                                }
                            }
                        )
                    }
                }

            }
        }
    }
}

