package com.dot.gallery.feature_node.presentation.photos

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dot.gallery.core.presentation.components.MediaComponent
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.ui.theme.Dimens

@Composable
fun PhotosScreen(
    navController: NavController,
    topPadding: Dp,
    viewModel: PhotosViewModel = hiltViewModel()
) {
    val state = viewModel.photoState.value
    val scope = rememberCoroutineScope()
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(Dimens.Photo()),
        contentPadding = PaddingValues(
            top = topPadding + 88.dp,
            bottom = 16.dp
        ),
        content = {
            items(state.media) { media ->
                MediaComponent(media = media) {
                    navController.navigate(Screen.MediaScreen.route + "?mediaId=${media.id}")
                }
            }
        }
    )
    if (state.media.isEmpty()) {
        Text(
            text = "Is Empty",
        )
    }
    if (state.isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.fillMaxSize()
        )
    }
    if (state.error.isNotEmpty()) Log.e("MediaError", state.error)
}