package com.dot.gallery.feature_node.presentation.mediaview

import android.app.Activity
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.MediaStoreSignature
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Constants.DEFAULT_LOW_VELOCITY_SWIPE_DURATION
import com.dot.gallery.core.presentation.components.MediaPreviewComponent
import com.dot.gallery.core.presentation.components.VideoPlayerController
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.mediaview.components.MediaViewAppBar
import com.dot.gallery.feature_node.presentation.mediaview.components.MediaViewBottomBar
import com.dot.gallery.feature_node.presentation.photos.PhotosViewModel
import com.dot.gallery.feature_node.presentation.util.getDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaViewScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    mediaId: Long,
    albumId: Long = -1L,
    viewModel: PhotosViewModel = hiltViewModel()
) {
    LaunchedEffect(albumId) {
        viewModel.albumId = albumId
    }

    val runtimeMediaId = remember { mutableStateOf(mediaId) }
    val state by remember {
        viewModel.photoState
    }
    val pagerState = rememberPagerState()
    val scrollEnabled = remember { mutableStateOf(true) }

    val currentDate = remember {
        mutableStateOf(
            state.media[pagerState.currentPage].timestamp.getDate("MMMM d, yyyy\nh:mm a")
        )
    }
    val currentMedia = remember { mutableStateOf(state.media[pagerState.currentPage]) }

    val showUI = remember { mutableStateOf(true) }
    val window = (LocalContext.current as Activity).window
    val windowInsetsController =
        WindowCompat.getInsetsController(window, window.decorView)
    val showUIListener: () -> Unit = {
        if (showUI.value)
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        else
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    val result = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = {}
    )
    val lastIndex = remember { mutableStateOf(-1) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            // do your stuff with selected page
            if (lastIndex.value != -1)
                runtimeMediaId.value = state.media[lastIndex.value].id
            currentDate.value = state.media[page].timestamp.getDate(
                "MMMM d, yyyy\nh:mm a"
            )
            currentMedia.value = state.media[page]
        }
    }

    LaunchedEffect(state.media) {
        if (lastIndex.value != -1)
            runtimeMediaId.value = state.media[lastIndex.value].id
        val index = state.media.indexOfFirst { it.id == runtimeMediaId.value }
        pagerState.scrollToPage(index)
        currentDate.value = state.media[index].timestamp.getDate(
            "MMMM d, yyyy\nh:mm a"
        )
        currentMedia.value = state.media[index]
    }
    Box(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize()
    ) {
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
                    .override(Target.SIZE_ORIGINAL)
                    .signature(MediaStoreSignature(null, item.timestamp, 0))
            }
            val (media, preloadRequestBuilder) = preloadingData[index]
            MediaPreviewComponent(
                media = media,
                scrollEnabled = scrollEnabled,
                playWhenReady = index == pagerState.currentPage,
                preloadRequestBuilder = preloadRequestBuilder,
                onItemClick = {
                    showUI.value = !showUI.value
                    showUIListener()
                }
            ) { player: ExoPlayer, currentTime: Long, totalTime: Long, buffer: Int, playToggle: () -> Unit ->
                AnimatedVisibility(
                    visible = showUI.value,
                    enter = enterAnimation(Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION),
                    exit = exitAnimation(Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION),
                    modifier = Modifier.fillMaxSize()
                ) {
                    VideoPlayerController(
                        paddingValues = paddingValues,
                        player = player,
                        currentTime = currentTime,
                        totalTime = totalTime,
                        buffer = buffer,
                        playToggle = playToggle
                    )
                }
            }
        }
        MediaViewAppBar(
            showUI = showUI.value,
            currentDate = currentDate.value,
            paddingValues = paddingValues,
            navController = navController
        )

        MediaViewBottomBar(
            showUI = showUI.value,
            paddingValues = paddingValues,
            currentMedia = currentMedia.value,
            currentIndex = pagerState.currentPage,
            result = result,
        ) {
            lastIndex.value = it
        }
    }

}