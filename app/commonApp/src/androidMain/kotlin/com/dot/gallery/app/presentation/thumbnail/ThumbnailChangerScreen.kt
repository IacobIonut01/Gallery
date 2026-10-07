package com.dot.gallery.app.presentation.thumbnail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.feature.albums.ui.ThumbnailChangerViewModel
import com.dot.gallery.injection.injectViewModel



@Composable
fun ThumbnailChangerScreen(
    navigateUp: () -> Unit,
) {
    val viewModel = injectViewModel<ThumbnailChangerViewModel> {thumbnailChangerViewModel}

    val hasAlbumThumbnail by viewModel.hasAlbumThumbnail.collectAsStateWithLifecycle()
    val albumThumbnail by viewModel.albumThumbnail.collectAsStateWithLifecycle()
    val albumDetails by viewModel.albumDetails.collectAsStateWithLifecycle()

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

        }
    }
}