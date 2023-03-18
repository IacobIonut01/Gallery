package com.dot.gallery.feature_node.presentation.albums

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.signature.MediaStoreSignature
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.presentation.albums.components.AlbumComponent
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.ui.theme.Dimens

@Composable
fun AlbumsScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    viewModel: AlbumsViewModel = hiltViewModel()
) {
    val state by rememberSaveable {
        viewModel.albumsState
    }
    val preloadingData = rememberGlidePreloadingData(
        data = state.albums,
        preloadImageSize = Size(50f, 50f)
    ) { item: Album, requestBuilder: RequestBuilder<Drawable> ->
        requestBuilder.load(item.pathToThumbnail)
            .signature(MediaStoreSignature(null, item.timestamp, 0))
    }
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(Dimens.Album()),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding() + 88.dp,
            start = 8.dp,
            end = 8.dp,
            bottom = paddingValues.calculateBottomPadding() + 16.dp
        ),
        content = {
            items(state.albums.size) { index ->
                val (album, preloadRequestBuilder) = preloadingData[index]
                AlbumComponent(album = album, preloadRequestBuilder) {
                    navController.navigate(Screen.AlbumsScreen.route + "?albumId=${album.id}")
                }
            }

        }
    )
    if (state.albums.isEmpty()) {
        Text(
            text = "Is Empty",
            modifier = Modifier
                .fillMaxSize()
        )
    }
    if (state.isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.fillMaxSize()
        )
    }
    if (state.error.isNotEmpty()) {
        Text(
            text = "An error occured",
            modifier = Modifier
                .fillMaxSize()
        )
        Log.e("MediaError", state.error)
    }
}