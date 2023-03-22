package com.dot.gallery.feature_node.presentation.mediaview

import android.app.Activity
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.MediaStoreSignature
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Constants.DEFAULT_LOW_VELOCITY_SWIPE_DURATION
import com.dot.gallery.core.Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION
import com.dot.gallery.core.presentation.components.MediaPreviewComponent
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.photos.PhotosViewModel
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.ui.theme.Black40P

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

    val currentDate = remember { mutableStateOf("") }
    currentDate.value = state.media[pagerState.currentPage].timestamp.getDate(
        "MMMM d, yyyy\nh:mm a"
    )

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


    LaunchedEffect(albumId) {
        viewModel.albumId = albumId
        val index = viewModel.photoState.value.media.indexOfFirst { it.id == mediaId }
        pagerState.scrollToPage(index)
        currentDate.value = state.media[index].timestamp.getDate(
            "MMMM d, yyyy\nh:mm a"
        )
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
                preloadRequestBuilder = preloadRequestBuilder
            ) {
                showUI.value = !showUI.value
                showUIListener()
            }
        }
        AnimatedVisibility(
            visible = showUI.value,
            enter = enterAnimation(DEFAULT_TOP_BAR_ANIMATION_DURATION),
            exit = exitAnimation(DEFAULT_TOP_BAR_ANIMATION_DURATION)
        ) {
            Row(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Black40P, Color.Transparent)
                        )
                    )
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Image(
                    imageVector = Icons.Outlined.ArrowBack,
                    colorFilter = ColorFilter.tint(Color.White),
                    contentDescription = "Go back",
                    modifier = Modifier
                        .height(48.dp)
                        .clickable {
                            navController.navigateUp()
                        }
                )
                Text(
                    text = currentDate.value.uppercase(),
                    modifier = Modifier,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    textAlign = TextAlign.End
                )
            }
        }
    }

}