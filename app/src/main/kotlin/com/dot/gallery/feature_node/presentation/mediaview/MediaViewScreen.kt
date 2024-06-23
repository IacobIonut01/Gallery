/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.core.AlbumState
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Constants.DEFAULT_LOW_VELOCITY_SWIPE_DURATION
import com.dot.gallery.core.Constants.HEADER_DATE_FORMAT
import com.dot.gallery.core.Constants.Target.TARGET_TRASH
import com.dot.gallery.core.MediaState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.mediaview.components.MediaViewAppBar
import com.dot.gallery.feature_node.presentation.mediaview.components.MediaViewBottomBar
import com.dot.gallery.feature_node.presentation.mediaview.components.media.MediaPreviewComponent
import com.dot.gallery.feature_node.presentation.mediaview.components.video.VideoPlayerController
import com.dot.gallery.feature_node.presentation.trashed.components.TrashedViewBottomBar
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.feature_node.presentation.util.rememberAppBottomSheetState
import com.dot.gallery.feature_node.presentation.util.rememberWindowInsetsController
import com.dot.gallery.feature_node.presentation.util.toggleSystemBars
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaViewScreen(
    navigateUp: () -> Unit,
    toggleRotate: () -> Unit,
    paddingValues: PaddingValues,
    isStandalone: Boolean = false,
    mediaId: Long,
    target: String? = null,
    mediaState: StateFlow<MediaState>,
    albumsState: StateFlow<AlbumState>,
    handler: MediaHandleUseCase,
    vaults: List<Vault>,
    addMedia: (Vault, Media) -> Unit
) {
    var runtimeMediaId by rememberSaveable(mediaId) { mutableLongStateOf(mediaId) }
    val state by mediaState.collectAsStateWithLifecycle()
    val albumState by albumsState.collectAsStateWithLifecycle()
    val initialPage = rememberSaveable(runtimeMediaId) {
        state.media.indexOfFirst { it.id == runtimeMediaId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        initialPageOffsetFraction = 0f,
        pageCount = state.media::size
    )
    val bottomSheetState = rememberAppBottomSheetState()

    val currentDate = rememberSaveable { mutableStateOf("") }
    val currentMedia = rememberSaveable { mutableStateOf<Media?>(null) }

    val showUI = rememberSaveable { mutableStateOf(true) }
    val windowInsetsController = rememberWindowInsetsController()

    var lastIndex by remember { mutableIntStateOf(-1) }
    val updateContent: (Int) -> Unit = { page ->
        if (state.media.isNotEmpty()) {
            val index = if (page == -1) 0 else page
            if (lastIndex != -1)
                runtimeMediaId = state.media[lastIndex.coerceAtMost(state.media.size - 1)].id
            currentDate.value = state.media[index].timestamp.getDate(HEADER_DATE_FORMAT)
            currentMedia.value = state.media[index]
        } else if (!isStandalone) navigateUp()
    }
    val scope = rememberCoroutineScope()

    val showInfo = remember(currentMedia.value) {
        currentMedia.value?.trashed == 0 && !(currentMedia.value?.readUriOnly ?: false)
    }

    LaunchedEffect(pagerState, state.media) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            updateContent(page)
        }
    }

    BackHandler(!showUI.value) {
        windowInsetsController.toggleSystemBars(show = true)
        navigateUp()
    }

    Box(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize()
    ) {
        HorizontalPager(
            modifier = Modifier
                .pointerInput(showInfo) {
                    detectVerticalDragGestures { change, dragAmount ->
                        if (showInfo && dragAmount < -5) {
                            change.consume()
                            scope.launch {
                                bottomSheetState.show()
                            }
                        }
                    }
                },
            state = pagerState,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                lowVelocityAnimationSpec = tween(
                    easing = FastOutLinearInEasing,
                    durationMillis = DEFAULT_LOW_VELOCITY_SWIPE_DURATION
                )
            ),
            key = { index ->
                if (state.media.isNotEmpty()) {
                    state.media[index.coerceIn(0 until state.media.size)].id
                } else "empty"
            },
            pageSpacing = 16.dp,
        ) { index ->
            var playWhenReady by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                snapshotFlow { pagerState.currentPage }
                    .collectLatest { currentPage ->
                        playWhenReady = currentPage == index
                    }
            }

            MediaPreviewComponent(
                media = state.media[index],
                uiEnabled = showUI.value,
                playWhenReady = playWhenReady,
                onItemClick = {
                    showUI.value = !showUI.value
                    windowInsetsController.toggleSystemBars(showUI.value)
                }
            ) { player, isPlaying, currentTime, totalTime, buffer, frameRate ->
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val displayMetrics = LocalContext.current.resources.displayMetrics

                    //Width And Height Of Screen
                    val width = displayMetrics.widthPixels
                    Spacer(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = width / 1.5f
                            }
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onDoubleClick = {
                                    scope.launch {
                                        currentTime.value += 10 * 1000
                                        player.seekTo(currentTime.value)
                                        delay(100)
                                        player.play()
                                    }
                                },
                                onClick = {
                                    showUI.value = !showUI.value
                                    windowInsetsController.toggleSystemBars(showUI.value)
                                }
                            )
                    )

                    Spacer(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = -width / 1.5f
                            }
                            .align(Alignment.TopStart)
                            .clip(CircleShape)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onDoubleClick = {
                                    scope.launch {
                                        currentTime.value -= 10 * 1000
                                        player.seekTo(currentTime.value)
                                        delay(100)
                                        player.play()
                                    }
                                },
                                onClick = {
                                    showUI.value = !showUI.value
                                    windowInsetsController.toggleSystemBars(showUI.value)
                                }
                            )
                    )

                    AnimatedVisibility(
                        visible = showUI.value,
                        enter = enterAnimation(Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION),
                        exit = exitAnimation(Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        VideoPlayerController(
                            paddingValues = paddingValues,
                            player = player,
                            isPlaying = isPlaying,
                            currentTime = currentTime,
                            totalTime = totalTime,
                            buffer = buffer,
                            toggleRotate = toggleRotate,
                            frameRate = frameRate
                        )
                    }
                }
            }
        }
        MediaViewAppBar(
            showUI = showUI.value,
            showInfo = showInfo,
            showDate = currentMedia.value?.timestamp != 0L,
            currentDate = currentDate.value,
            bottomSheetState = bottomSheetState,
            paddingValues = paddingValues,
            onGoBack = navigateUp
        )
        if (target == TARGET_TRASH) {
            TrashedViewBottomBar(
                handler = handler,
                showUI = showUI.value,
                paddingValues = paddingValues,
                currentMedia = currentMedia.value,
                currentIndex = pagerState.currentPage
            ) {
                lastIndex = it
            }
        } else {
            MediaViewBottomBar(
                showDeleteButton = remember(currentMedia.value) {
                    currentMedia.value?.readUriOnly == false
                },
                bottomSheetState = bottomSheetState,
                handler = handler,
                showUI = showUI.value,
                paddingValues = paddingValues,
                currentMedia = currentMedia.value,
                albumsState = albumState,
                currentIndex = pagerState.currentPage,
                addMedia = addMedia,
                vaults = vaults
            ) {
                lastIndex = it
            }
        }
    }

}