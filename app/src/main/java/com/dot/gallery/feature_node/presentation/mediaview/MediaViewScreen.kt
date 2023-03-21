package com.dot.gallery.feature_node.presentation.mediaview

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.signature.MediaStoreSignature
import com.dot.gallery.core.Constants.DEFAULT_LOW_VELOCITY_SWIPE_DURATION
import com.dot.gallery.core.presentation.components.MediaPreviewComponent
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.photos.PhotosViewModel

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
        flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            lowVelocityAnimationSpec = tween(
                easing = LinearEasing,
                durationMillis = DEFAULT_LOW_VELOCITY_SWIPE_DURATION
            )
        ),
        pageSpacing = 16.dp,
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize()
    ) { index ->
        val preloadingData = rememberGlidePreloadingData(
            data = state.media,
            preloadImageSize = Size(200f, 200f)
        ) { item: Media, requestBuilder: RequestBuilder<Drawable> ->
            requestBuilder.load(item.uri)
                .signature(MediaStoreSignature(null, item.timestamp, 0))
        }
        val (media, preloadRequestBuilder) = preloadingData[index]
        MediaPreviewComponent(
            media = media,
            scrollEnabled = scrollEnabled,
            playWhenReady = index == pagerState.currentPage,
            preloadRequestBuilder = preloadRequestBuilder
        )
    }
}