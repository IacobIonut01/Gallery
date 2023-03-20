package com.dot.gallery.feature_node.presentation.photos

import android.Manifest
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.signature.MediaStoreSignature
import com.dot.gallery.R
import com.dot.gallery.core.presentation.components.MediaComponent
import com.dot.gallery.core.presentation.components.Toolbar
import com.dot.gallery.core.presentation.components.util.header
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.ui.theme.Dimens
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PhotosScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    albumId: Long = -1L,
    albumName: String = stringResource(id = R.string.app_name),
    viewModel: PhotosViewModel = hiltViewModel(),
) {
    val mediaPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    )
    if (!mediaPermissions.allPermissionsGranted) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { mediaPermissions.launchMultiplePermissionRequest() }
            ) {
                Text(text = "Request permissions")
            }
        }
    } else {
        LaunchedEffect(albumId) {
            viewModel.albumId = albumId
        }
        val state by remember {
            viewModel.photoState
        }
        val stringToday = stringResource(id = R.string.header_today)
        val stringYesterday = stringResource(id = R.string.header_yesterday)
        val gridState = rememberLazyGridState()
        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Adaptive(Dimens.Photo()),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            )
        ) {
            header {
                Toolbar(
                    navController = navController,
                    text = albumName
                )
            }
            val list = state.media.groupBy {
                it.timestamp.getDate(
                    stringToday = stringToday,
                    stringYesterday = stringYesterday
                )
            }
            list.forEach { (date, data) ->
                header {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(
                                horizontal = 16.dp,
                                vertical = 24.dp
                            )
                            .fillMaxWidth()
                    )
                }
                items(data.size) { index ->
                    val preloadingData = rememberGlidePreloadingData(
                        data = data,
                        preloadImageSize = Size(50f, 50f)
                    ) { item: Media, requestBuilder: RequestBuilder<Drawable> ->
                        requestBuilder.load(item.uri)
                            .signature(MediaStoreSignature(null, item.timestamp, 0))
                    }
                    val (media, preloadRequestBuilder) = preloadingData[index]
                    MediaComponent(media = media, preloadRequestBuilder) {
                        navController.navigate(Screen.MediaViewScreen.route + "?mediaId=${media.id}&albumId=${albumId}")
                    }
                }
            }
        }
        if (state.media.isEmpty()) {
            Text(
                text = "Is Empty",
                modifier = Modifier
                    .fillMaxSize()
            )
            viewModel.viewModelScope.launch {
                viewModel.getMedia(albumId)
            }
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
}