package com.dot.gallery.feature_node.presentation.mediaview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dot.gallery.core.presentation.components.MediaPreviewComponent
import com.dot.gallery.feature_node.presentation.photos.PhotosViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaViewScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    mediaId: Long,
    albumId: Long = -1L,
    viewModel: PhotosViewModel = hiltViewModel()
) {
    val state by remember {
        viewModel.photoState
    }
    val pagerState = rememberPagerState()
    val scrollEnabled = remember { mutableStateOf(true) }

    LaunchedEffect(albumId) {
        viewModel.albumId = albumId
        pagerState.scrollToPage(viewModel.photoState.value.media.indexOfFirst { it.id == mediaId })
    }

    HorizontalPager(
        state = pagerState,
        pageCount = state.media.size,
        userScrollEnabled = scrollEnabled.value,
        pageSpacing = 16.dp,
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize()
    ) { index ->
        MediaPreviewComponent(
            media = state.media[index],
            scrollEnabled = scrollEnabled,
            playWhenReady = index == pagerState.currentPage,
        )
    }
}