package com.dot.gallery.feature_node.presentation.photos

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dot.gallery.R
import com.dot.gallery.core.presentation.components.MediaComponent
import com.dot.gallery.core.presentation.components.util.header
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.feature_node.presentation.util.updateDate
import com.dot.gallery.ui.theme.Dimens

@Composable
fun PhotosScreen(
    navController: NavController,
    topPadding: Dp,
    viewModel: PhotosViewModel = hiltViewModel()
) {
    val state by rememberSaveable {
        viewModel.photoState
    }
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(Dimens.Photo()),
        contentPadding = PaddingValues(
            top = topPadding + 88.dp,
            bottom = 16.dp
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
                items(data) { media ->
                    MediaComponent(media = media) {
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