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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.dot.gallery.core.presentation.components.util.header
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.feature_node.presentation.util.updateDate
import com.dot.gallery.ui.theme.Dimens
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PhotosScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    viewModel: PhotosViewModel = hiltViewModel()
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
    val state by remember {
        viewModel.photoState
    }

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(Dimens.Photo()),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding() + 88.dp,
            bottom = paddingValues.calculateBottomPadding() + 16.dp
        ),
        content = {
            val list = state.media.groupBy {
                it.timestamp.getDate()
            }
            list.forEach { (date, data) ->
                header {
                    Text(
                        text = updateDate(date, stringResource(id = R.string.header_today)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(all = 16.dp)
                            .fillMaxWidth()
                    )
                }
                items(data.size) { index ->
                    val preloadingData = rememberGlidePreloadingData(
                        numberOfItemsToPreload = data.size / 4,
                        data = data,
                        preloadImageSize = Size(50f, 50f)
                    ) { item: Media, requestBuilder: RequestBuilder<Drawable> ->
                        requestBuilder.load(item.uri)
                            .signature(MediaStoreSignature(null, item.timestamp, 0))
                    }
                    val (media, preloadRequestBuilder) = preloadingData[index]
                    MediaComponent(media = media, preloadRequestBuilder) {
                        navController.navigate(Screen.MediaScreen.route + "?mediaId=${media.id}")
                    }
                }
            }

        }
    )
    if (state.media.isEmpty()) {
        Text(
            text = "Is Empty",
            modifier = Modifier
                .fillMaxSize()
        )
            viewModel.viewModelScope.launch {
                viewModel.getMedia()
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