/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.composables.core.BottomSheet
import com.composables.core.SheetDetent.Companion.FullyExpanded
import com.composables.core.rememberBottomSheetState
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Constants.DEFAULT_LOW_VELOCITY_SWIPE_DURATION
import com.dot.gallery.core.Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION
import com.dot.gallery.core.Constants.HEADER_DATE_FORMAT
import com.dot.gallery.core.Constants.Target.TARGET_TRASH
import com.dot.gallery.core.Settings.Misc.rememberAutoHideOnVideoPlay
import com.dot.gallery.feature_node.domain.model.AlbumState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaState
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.model.VaultState
import com.dot.gallery.feature_node.domain.model.readUriOnly
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.mediaview.components.MediaViewActions2
import com.dot.gallery.feature_node.presentation.mediaview.components.MediaViewAppBar
import com.dot.gallery.feature_node.presentation.mediaview.components.MediaViewDetails
import com.dot.gallery.feature_node.presentation.mediaview.components.TrackVisibility
import com.dot.gallery.feature_node.presentation.mediaview.components.media.MediaPreviewComponent
import com.dot.gallery.feature_node.presentation.mediaview.components.video.VideoPlayerController
import com.dot.gallery.feature_node.presentation.util.FullBrightnessWindow
import com.dot.gallery.feature_node.presentation.util.ViewScreenConstants.BOTTOM_BAR_HEIGHT
import com.dot.gallery.feature_node.presentation.util.ViewScreenConstants.BOTTOM_BAR_HEIGHT_SLIM
import com.dot.gallery.feature_node.presentation.util.ViewScreenConstants.FullyExpanded
import com.dot.gallery.feature_node.presentation.util.ViewScreenConstants.ImageOnly
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.feature_node.presentation.util.normalize
import com.dot.gallery.feature_node.presentation.util.rememberWindowInsetsController
import com.dot.gallery.feature_node.presentation.util.toggleSystemBars
import com.dot.gallery.ui.theme.BlackScrim
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Stable
@Composable
fun MediaViewScreen(
    navigateUp: () -> Unit,
    toggleRotate: () -> Unit,
    paddingValues: PaddingValues,
    isStandalone: Boolean = false,
    mediaId: Long,
    target: String? = null,
    mediaState: State<MediaState>,
    albumsState: State<AlbumState>,
    handler: MediaHandleUseCase,
    vaultState: State<VaultState>,
    addMedia: (Vault, Media) -> Unit
) {
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    val initialPage by rememberSaveable(mediaId, mediaState.value, currentPage) {
        var lastMediaPosition = mediaState.value.media.indexOfFirst { it.id == mediaId }
        if (currentPage != 0) {
            lastMediaPosition = currentPage
        }
        mutableIntStateOf(lastMediaPosition.coerceAtLeast(0))
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        initialPageOffsetFraction = 0f,
        pageCount = mediaState.value.media::size
    )

    val currentDate = rememberSaveable { mutableStateOf("") }
    val currentMedia = rememberSaveable { mutableStateOf<Media?>(null) }
    val isReadOnly by remember(currentMedia.value) {
        derivedStateOf { currentMedia.value?.readUriOnly == true }
    }
    val scope = rememberCoroutineScope()

    val showUI = rememberSaveable { mutableStateOf(true) }
    val windowInsetsController = rememberWindowInsetsController()

    var lastIndex by remember { mutableIntStateOf(-1) }

    val showInfo by remember(currentMedia.value) {
        derivedStateOf { currentMedia.value?.trashed == 0 && !isReadOnly }
    }

    BackHandler(!showUI.value) {
        windowInsetsController.toggleSystemBars(show = true)
        navigateUp()
    }
    var sheetHeightDp by remember { mutableStateOf(0.dp) }
    var lastSheetHeightDp by remember { mutableStateOf(sheetHeightDp) }
    val sheetState = rememberBottomSheetState(
        initialDetent = ImageOnly,
        detents = listOf(ImageOnly, FullyExpanded { sheetHeightDp = it })
    )

    var normalizationTarget by remember {
        mutableFloatStateOf(0f)
    }
    var isNormalizationTargetSet by remember { mutableStateOf(false) }

    LaunchedEffect(mediaState.value) {
        snapshotFlow { pagerState.currentPage }.collectLatest { page ->
            if (lastIndex != -1) {
                val newIndex = lastIndex.coerceAtMost(pagerState.pageCount - 1)
                pagerState.scrollToPage(newIndex)
                lastIndex = -1
            }
            if (page != currentPage) {
                isNormalizationTargetSet = false
            }

            currentMedia.value = mediaState.value.media.getOrNull(page)
            currentDate.value = currentMedia.value?.timestamp?.getDate(HEADER_DATE_FORMAT) ?: ""

            if (!mediaState.value.isLoading && mediaState.value.media.isEmpty() && !isStandalone) {
                windowInsetsController.toggleSystemBars(show = true)
                navigateUp()
            }
            if (!mediaState.value.isLoading) {
                currentPage = page
            }
        }
    }
    LaunchedEffect(sheetHeightDp) {
        if (lastSheetHeightDp != sheetHeightDp) {
            lastSheetHeightDp = sheetHeightDp
            isNormalizationTargetSet = false
        }
    }

    LaunchedEffect(isNormalizationTargetSet) {
        snapshotFlow { sheetState.offset }.collectLatest { offset ->
            if (!isNormalizationTargetSet) {
                isNormalizationTargetSet = true
                normalizationTarget = offset
            }
        }
    }

    val normalizedOffset by remember(normalizationTarget) {
        derivedStateOf {
            if (isNormalizationTargetSet) {
                sheetState.offset.normalize(minValue = normalizationTarget)
            } else 0f
        }
    }
    val bottomPadding = remember {
        paddingValues.calculateBottomPadding()
    }


    FullBrightnessWindow {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY =
                            -((sheetHeightDp - BOTTOM_BAR_HEIGHT - bottomPadding).toPx() * normalizedOffset)
                    },
                state = pagerState,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapAnimationSpec = tween(
                        easing = FastOutLinearInEasing,
                        durationMillis = DEFAULT_LOW_VELOCITY_SWIPE_DURATION
                    )
                ),
                key = { index ->
                    mediaState.value.media.getOrNull(index) ?: "empty"
                },
                pageSpacing = 16.dp,
            ) { index ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var playWhenReady by remember { mutableStateOf(false) }
                    val media by remember(index, mediaState) {
                        derivedStateOf { mediaState.value.media.getOrNull(index) }
                    }
                    TrackVisibility(
                        onVisibilityChanged = { isVisible ->
                            playWhenReady = isVisible
                        }
                    ) {
                        AnimatedVisibility(
                            visible = remember(media) { media != null },
                            enter = enterAnimation,
                            exit = exitAnimation
                        ) {
                            MediaPreviewComponent(
                                media = media!!,
                                uiEnabled = showUI.value,
                                playWhenReady = playWhenReady,
                                onSwipeDown = {
                                    windowInsetsController.toggleSystemBars(show = true)
                                    navigateUp()
                                },
                                onItemClick = {
                                    if (sheetState.currentDetent == ImageOnly) {
                                        showUI.value = !showUI.value
                                        windowInsetsController.toggleSystemBars(showUI.value)
                                    }
                                }
                            ) { player, isPlaying, currentTime, totalTime, buffer, frameRate ->
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    val hideUiOnPlay by rememberAutoHideOnVideoPlay()
                                    LaunchedEffect(isPlaying.value, hideUiOnPlay) {
                                        if (isPlaying.value && showUI.value && hideUiOnPlay) {
                                            delay(2.seconds)
                                            showUI.value = false
                                            windowInsetsController.toggleSystemBars(false)
                                        }
                                    }

                                    val context = LocalContext.current
                                    val width =
                                        remember(context) { context.resources.displayMetrics.widthPixels }
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
                                                        currentTime.longValue += 10 * 1000
                                                        player.seekTo(currentTime.longValue)
                                                        delay(100)
                                                        player.play()
                                                    }
                                                },
                                                onClick = {
                                                    if (sheetState.currentDetent == ImageOnly) {
                                                        showUI.value = !showUI.value
                                                        windowInsetsController.toggleSystemBars(
                                                            showUI.value
                                                        )
                                                    }
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
                                                        currentTime.longValue -= 10 * 1000
                                                        player.seekTo(currentTime.longValue)
                                                        delay(100)
                                                        player.play()
                                                    }
                                                },
                                                onClick = {
                                                    if (sheetState.currentDetent == ImageOnly) {
                                                        showUI.value = !showUI.value
                                                        windowInsetsController.toggleSystemBars(
                                                            showUI.value
                                                        )
                                                    }
                                                }
                                            )
                                    )

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = showUI.value,
                                        enter = enterAnimation(DEFAULT_TOP_BAR_ANIMATION_DURATION),
                                        exit = exitAnimation(DEFAULT_TOP_BAR_ANIMATION_DURATION),
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
                    }
                }
            }
            MediaViewAppBar(
                showUI = showUI.value,
                showInfo = showInfo,
                showDate = remember(currentMedia.value) {
                    currentMedia.value?.timestamp != 0L
                },
                currentDate = currentDate.value,
                paddingValues = paddingValues,
                onShowInfo = {
                    scope.launch {
                        if (showUI.value) {
                            if (sheetState.currentDetent == ImageOnly) {
                                sheetState.animateTo(FullyExpanded)
                            } else {
                                sheetState.animateTo(ImageOnly)
                            }
                        }
                    }
                },
                onGoBack = {
                    scope.launch {
                        if (sheetState.currentDetent == FullyExpanded) {
                            sheetState.animateTo(ImageOnly)
                        } else {
                            navigateUp()
                        }
                    }
                }
            )
            LaunchedEffect(showUI.value) {
                if (!showUI.value && (sheetState.currentDetent == FullyExpanded || sheetState.targetDetent == FullyExpanded)) {
                    sheetState.animateTo(ImageOnly)
                }
            }
            BackHandler(sheetState.currentDetent == FullyExpanded) {
                scope.launch {
                    sheetState.animateTo(ImageOnly)
                }
            }
            val bottomSheetAlpha by animateFloatAsState(
                targetValue = if (showUI.value) 1f else 0f,
                animationSpec = tween(DEFAULT_TOP_BAR_ANIMATION_DURATION),
                label = "MediaViewActionsAlpha"
            )
            BottomSheet(
                state = sheetState,
                enabled = showUI.value && target != TARGET_TRASH,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .alpha(bottomSheetAlpha)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val alpha by animateFloatAsState(
                        targetValue = 1f - normalizedOffset,
                        label = "MediaViewActions2Alpha"
                    )
                    AnimatedVisibility(
                        visible = currentMedia.value != null,
                        enter = enterAnimation,
                        exit = exitAnimation
                    ) {
                        Row(
                            modifier = Modifier
                                .graphicsLayer {
                                    this.alpha = alpha
                                    translationY =
                                        BOTTOM_BAR_HEIGHT_SLIM.toPx() * normalizedOffset
                                }
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, BlackScrim)
                                    )
                                )
                                .padding(
                                    bottom = bottomPadding
                                )
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            if (currentMedia.value != null) {
                                MediaViewActions2(
                                    currentIndex = pagerState.currentPage,
                                    currentMedia = currentMedia.value!!,
                                    handler = handler,
                                    onDeleteMedia = { lastIndex = it },
                                    showDeleteButton = !isReadOnly,
                                    enabled = showUI.value
                                )
                            }
                        }
                    }

                    MediaViewDetails(
                        handler = handler,
                        albumsState = albumsState,
                        currentMedia = currentMedia.value,
                        vaultState = vaultState,
                        addMediaToVault = addMedia
                    )
                }
            }
        }
    }

}