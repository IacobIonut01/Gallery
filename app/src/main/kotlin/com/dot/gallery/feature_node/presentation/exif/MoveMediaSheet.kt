package com.dot.gallery.feature_node.presentation.exif

import android.media.MediaScannerConnection
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Settings.Album.rememberAlbumSize
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.albums.AlbumsViewModel
import com.dot.gallery.feature_node.presentation.albums.components.AlbumComponent
import com.dot.gallery.feature_node.presentation.util.AppBottomSheetState
import com.dot.gallery.feature_node.presentation.util.Error
import com.dot.gallery.feature_node.presentation.util.rememberActivityResult
import com.dot.gallery.feature_node.presentation.util.writeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MoveMediaSheet(
    sheetState: AppBottomSheetState,
    mediaList: List<Media>,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val toastError = Toast::class.Error()
    val albumViewModel = hiltViewModel<AlbumsViewModel>()
    albumViewModel.attachToLifecycle()

    val scope = rememberCoroutineScope()
    val state by albumViewModel.albumsState.collectAsStateWithLifecycle()
    val handler = albumViewModel.handler
    var progress by remember(mediaList) { mutableFloatStateOf(0f) }
    var newPath by remember(mediaList) { mutableStateOf("") }

    val request = rememberActivityResult {
        scope.launch {
            val done = async {
                mediaList.forEachIndexed { index, it ->
                    if (handler.moveMedia(media = it, newPath = newPath)) {
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(newPath),
                            arrayOf(it.mimeType),
                            null
                        )
                        progress = index.toFloat() / mediaList.size
                    } else {
                        return@async false
                    }
                }
                return@async true
            }
            if (done.await()) {
                sheetState.hide()
                onFinish()
            } else {
                toastError.show()
                delay(1000)
                sheetState.hide()
            }
        }
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
                    text = stringResource(R.string.move_to_another_album),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                )

                val albumSize by rememberAlbumSize()
                LazyVerticalGrid(
                    state = rememberLazyGridState(),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    columns = GridCells.Adaptive(Dp(albumSize)),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = state.albums,
                        key = { item -> item.toString() }
                    ) { item ->
                        val mediaVolume = (mediaList.getOrNull(0)?.volume ?: item.volume)
                        val albumOwnership = item.relativePath.substringBeforeLast("Android/media/", "allow")
                        val mediaOwnership = mediaList.getOrNull(0)?.relativePath?.substringBeforeLast("Android/media/", "allow") ?: albumOwnership
                        val mediaAlbum = mediaList.getOrNull(0)?.albumLabel ?: item.label
                        AlbumComponent(
                            album = item,
                            isEnabled = item.volume == mediaVolume
                                    && albumOwnership == "allow"
                                    && mediaOwnership == "allow"
                                    && item.label != mediaAlbum,
                            onItemClick = { album ->
                                scope.launch(Dispatchers.Main) {
                                    newPath = album.relativePath
                                    request.launch(mediaList.writeRequest(context.contentResolver))
                                }
                            },
                            onTogglePinClick = null
                        )
                    }
                }
                AnimatedVisibility(
                    visible = progress > 0f,
                    enter = enterAnimation,
                    exit = exitAnimation
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        progress = progress
                    )
                }
            }
        }
    }
}