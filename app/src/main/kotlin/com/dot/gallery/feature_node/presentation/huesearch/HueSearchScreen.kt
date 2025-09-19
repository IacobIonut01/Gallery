package com.dot.gallery.feature_node.presentation.huesearch

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.Scanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.LocalMediaSelector
import com.dot.gallery.core.navigate
import com.dot.gallery.feature_node.presentation.common.MediaScreen
import com.dot.gallery.feature_node.presentation.edit.components.core.SupportiveLayout
import com.dot.gallery.feature_node.presentation.edit.components.filters.horizontalSystemGesturesPadding
import com.dot.gallery.feature_node.presentation.edit.components.markup.HueBar
import com.dot.gallery.feature_node.presentation.library.components.LibrarySmallItem
import com.dot.gallery.feature_node.presentation.library.components.dashedBorder
import com.dot.gallery.feature_node.presentation.util.Screen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.HueSearchScreen(
    paddingValues: PaddingValues,
    clearSelection: () -> Unit,
    animatedContentScope: AnimatedContentScope,
    viewModel: HueSearchViewModel
) {

    val currentHue by viewModel.hueState.collectAsStateWithLifecycle()
    val mediaState = viewModel.imagesByHue.collectAsStateWithLifecycle()
    val indexedImageCount by viewModel.indexedImageCount.collectAsStateWithLifecycle()
//    val metadataState = viewModel.metadataFlow.collectAsStateWithLifecycle()
//
//    val eventHandler = LocalEventHandler.current
//    val selector = LocalMediaSelector.current
//
//    val selectionState = selector.isSelectionActive.collectAsStateWithLifecycle()
//    val selectedMedia = selector.selectedMedia.collectAsStateWithLifecycle()
//
//    var canScroll by rememberSaveable { mutableStateOf(true) }
//    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
//        state = rememberTopAppBarState(),
//        canScroll = { canScroll },
//        flingAnimationSpec = null
//    )
//    var lastCellIndex by rememberGridSize()
//
//    val pinchState = rememberPinchZoomGridState(
//        cellsList = cellsList,
//        initialCellsIndex = lastCellIndex
//    )
//
//    LaunchedEffect(pinchState.isZooming) {
//        withContext(Dispatchers.IO) {
//            canScroll = !pinchState.isZooming
//            lastCellIndex = cellsList.indexOf(pinchState.currentCells)
//        }
//    }
    val eventHandler = LocalEventHandler.current

    Box(Modifier.fillMaxSize()) {
        MediaScreen(
            paddingValues = paddingValues,
            albumName = stringResource(R.string.search_by_hue),
            target = "hue_$currentHue",
            mediaState = mediaState,
            allowHeaders = false,
            enableStickyHeaders = false,
            navActionsContent = { _: MutableState<Boolean>,
                                  result: ActivityResultLauncher<IntentSenderRequest> ->
            },
            aboveGridContent = {
                val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
                val progress by viewModel.progress.collectAsStateWithLifecycle()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (indexedImageCount == 0 && !isRunning) {
                        NoHueInfo(onClick = viewModel::startIndexing)
                    }

                    if (indexedImageCount != 0 || isRunning) {
                        ScannerButton(
                            isRunning = isRunning,
                            indicatorCounter = progress,
                            contentColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onLongClick = {
                                        if (isRunning) viewModel.stopIndexing()
                                    },
                                    onClick = {
                                        if (!isRunning) viewModel.startIndexing()
                                    }
                                )
                        )
                    }

                    if (indexedImageCount != 0 && !isRunning) {
                        LibrarySmallItem(
                            title = "Delete all hue info",
                            icon = Icons.Default.Delete,
                            contentColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .clickable(onClick = viewModel::deleteHueIndexData)
                        )
                    }
                }
            },
            sharedTransitionScope = this@HueSearchScreen,
            animatedContentScope = animatedContentScope,
            customViewingNavigation = { media ->
                eventHandler.navigate(Screen.MediaViewScreen.idAndHue(media.id, currentHue.toArgb()))
            }
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                clearSelection()
            }
        }

        SupportiveLayout(
            isSupportingPanel = false,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(paddingValues)
                .padding(bottom = 36.dp)
        ) {
            val selector = LocalMediaSelector.current
            val isSelectionActive by selector.isSelectionActive.collectAsStateWithLifecycle()

            AnimatedVisibility(!isSelectionActive) {
                HueBar(
                    modifier = Modifier.padding(WindowInsets.horizontalSystemGesturesPadding()),
                    currentColor = currentHue,
                    isSupportingPanel = false,
                    enabled = true
                ) { hue ->
                    val hsv = FloatArray(3)
                    android.graphics.Color.colorToHSV(currentHue.toArgb(), hsv)
                    hsv[0] = hue
                    val newColor = Color(
                        android.graphics.Color.HSVToColor(
                            (currentHue.alpha * 255).toInt(),
                            hsv
                        )
                    )
                    viewModel.setColor(newColor)
                }
            }
        }
    }


//    Box {
//        val scaffoldModifier =
//            remember { Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) }
//        Scaffold(
//            modifier = scaffoldModifier,
//            topBar = {
//                TopAppBar(
//                    title = {
//                        TwoLinedDateToolbarTitle(
//                            albumName = stringResource(R.string.search_by_hue),
//                            dateHeader = stringResource(
//                                R.string.classified_media,
//                                indexedImageCount
//                            )
//                        )
//                    },
//                    navigationIcon = {
//                        NavigationButton(
//                            albumId = -1L,
//                            target = "hue=$currentHue",
//                            alwaysGoBack = false,
//                        )
//                    },
//                    scrollBehavior = scrollBehavior
//                )
//            }
//        ) { paddingValues ->
//            Column(
//                modifier = Modifier
//                    .padding(paddingValues)
//                    .fillMaxWidth()
//                    .animateContentSize(),
//            ) {
//                val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
//                val progress by viewModel.progress.collectAsStateWithLifecycle()
//
//                Column(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(16.dp),
//                    verticalArrangement = Arrangement.spacedBy(16.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    if (indexedImageCount == 0 && !isRunning) {
//                        NoHueInfo(onClick = viewModel::startIndexing)
//                    }
//
//                    if (indexedImageCount != 0 || isRunning) {
//                        ScannerButton(
//                            isRunning = isRunning,
//                            indicatorCounter = progress,
//                            contentColor = MaterialTheme.colorScheme.tertiary,
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .combinedClickable(
//                                    onLongClick = {
//                                        if (isRunning) viewModel.stopIndexing()
//                                    },
//                                    onClick = {
//                                        if (!isRunning) viewModel.startIndexing()
//                                    }
//                                )
//                        )
//                    }
//
//                    if (indexedImageCount != 0 && !isRunning) {
//                        LibrarySmallItem(
//                            title = "Delete all hue info",
//                            icon = Icons.Default.Delete,
//                            contentColor = MaterialTheme.colorScheme.error,
//                            modifier = Modifier
//                                .clickable(onClick = viewModel::deleteHueIndexData)
//                        )
//                    }
//                }
//
//                PinchZoomGridLayout(pinchState) {
//                    if (!isRunning) {
//                        MediaGridView(
//                            mediaState = mediaState,
//                            metadataState = metadataState,
//                            isScrolling = isScrolling,
//                            allowHeaders = false,
//                            allowSelection = true,
//                            emptyContent = { EmptyMedia() },
//                            sharedTransitionScope = this@HueSearchScreen,
//                            animatedContentScope = animatedContentScope,
//                            onMediaClick = { eventHandler.navigate(Screen.MediaViewScreen.route + "?mediaId=${it.id}&hue=${currentHue.toArgb()}") }
//                        )
//                    } else {
//                        EmptyMedia(title = "Kindly wait for the scan to complete...")
//                    }
//                }
//            }
//        }
//        SupportiveLayout(
//            isSupportingPanel = false,
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .padding(bottom = 36.dp)
//        ) {
//            AnimatedVisibility(!selectionState.value) {
//                HueBar(
//                    modifier = Modifier.padding(WindowInsets.horizontalSystemGesturesPadding()),
//                    currentColor = currentHue,
//                    isSupportingPanel = false,
//                    enabled = true
//                ) { hue ->
//                    val hsv = FloatArray(3)
//                    android.graphics.Color.colorToHSV(currentHue.toArgb(), hsv)
//                    hsv[0] = hue
//                    val newColor = Color(
//                        android.graphics.Color.HSVToColor(
//                            (currentHue.alpha * 255).toInt(),
//                            hsv
//                        )
//                    )
//                    viewModel.setColor(newColor)
//                }
//            }
//        }
//        AnimatedVisibility(
//            modifier = Modifier
//                .align(Alignment.BottomEnd),
//            visible = selectionState.value,
//            enter = enterAnimation,
//            exit = exitAnimation
//        ) {
//            val selectedMediaList = mediaState.value.media.selectedMedia(selectedSet = selectedMedia)
//            SelectionSheet(
//                modifier = Modifier
//                    .align(Alignment.BottomEnd),
//                allMedia = mediaState.value,
//                selectedMedia = selectedMediaList
//            )
//        }
//    }
}

@Composable
fun ScannerButton(
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
    indicatorCounter: Float = 0f,
    isRunning: Boolean = false
) {
    ListItem(
        colors = ListItemDefaults.colors(
            containerColor = contentColor.copy(alpha = 0.1f),
            headlineColor = contentColor
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .then(modifier),
        headlineContent = {
            val scanningMediaText = "Scanning photos for hue info..."
            val scanForNewCategoriesText = "Scan photos for hue info"
            val text = remember(isRunning) {
                if (isRunning) scanningMediaText else scanForNewCategoriesText
            }
            Text(
                modifier = Modifier
                    .then(if (isRunning) Modifier.padding(top = 8.dp) else Modifier),
                text = text,
                style = MaterialTheme.typography.labelLarge,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Scanner,
                tint = contentColor,
                contentDescription = stringResource(R.string.scan_for_new_categories)
            )
        },
        trailingContent = {
            AnimatedVisibility(
                visible = isRunning,
                enter = enterAnimation,
                exit = exitAnimation
            ) {
                Text(
                    text = remember(indicatorCounter) {
                        String.format(
                            Locale.getDefault(),
                            "%.1f",
                            indicatorCounter.coerceIn(0f..100f)
                        ) + "%"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        },
        supportingContent = if (isRunning) {
            {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    AnimatedVisibility(
                        visible = indicatorCounter < 100f
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            progress = { (indicatorCounter / 100f).coerceAtLeast(0f) },
                            color = contentColor,
                        )
                    }

                    AnimatedVisibility(
                        visible = indicatorCounter == 100f
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = contentColor,
                        )
                    }
                }
            }
        } else null
    )
}

@Composable
fun NoHueInfo(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
        )
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .dashedBorder(
                brush = brush,
                shape = RoundedCornerShape(16.dp),
                gapLength = 8.dp
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = rememberVectorPainter(image = Icons.Outlined.ImageSearch),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .drawWithContent {
                    with(drawContext.canvas.nativeCanvas) {
                        val checkPoint = saveLayer(null, null)
                        drawContent()
                        drawRect(
                            brush = brush,
                            blendMode = BlendMode.SrcIn
                        )
                        restoreToCount(checkPoint)
                    }
                }
        )
        Text(
            text = "Scan your photos for hue info",
            style = MaterialTheme.typography.titleMedium.copy(brush = brush),
        )
    }
}