package com.dot.gallery.feature_node.presentation.huesearch

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dokar.pinchzoomgrid.PinchZoomGridLayout
import com.dokar.pinchzoomgrid.rememberPinchZoomGridState
import com.dot.gallery.R
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Constants.cellsList
import com.dot.gallery.core.Settings.Misc.rememberGridSize
import com.dot.gallery.core.presentation.components.EmptyMedia
import com.dot.gallery.core.presentation.components.NavigationButton
import com.dot.gallery.core.presentation.components.SelectionSheet
import com.dot.gallery.feature_node.domain.model.AlbumState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaMetadataState
import com.dot.gallery.feature_node.domain.model.MediaState
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.common.components.MediaGridView
import com.dot.gallery.feature_node.presentation.common.components.TwoLinedDateToolbarTitle
import com.dot.gallery.feature_node.presentation.edit.components.core.SupportiveLayout
import com.dot.gallery.feature_node.presentation.edit.components.filters.horizontalSystemGesturesPadding
import com.dot.gallery.feature_node.presentation.edit.components.markup.HueBar
import com.dot.gallery.feature_node.presentation.library.components.LibrarySmallItem
import com.dot.gallery.feature_node.presentation.library.components.dashedBorder
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.feature_node.presentation.util.clear
import com.dot.gallery.feature_node.presentation.util.selectedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HueSearchScreen(
    setColor: (Color) -> Unit,
    currentColor: State<Color>,
    isRunning: State<Boolean>,
    progress: State<Float>,
    classifiedCount: State<Int>,
    startClassification: () -> Unit,
    stopClassification: () -> Unit,
    deleteClassifications: () -> Unit,
    mediaState: State<MediaState<Media.HueClassifiedMedia>>,
    metadataState: State<MediaMetadataState>,
    handler: MediaHandleUseCase,
    albumsState: State<AlbumState>,
    selectionState: MutableState<Boolean>,
    selectedMedia: MutableState<Set<Long>>,
    toggleSelection: (MediaState<Media.HueClassifiedMedia>, Int) -> Unit,
    navigate: (route: String) -> Unit,
    navigateUp: () -> Unit,
    toggleNavbar: (Boolean) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    var canScroll by rememberSaveable { mutableStateOf(true) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
        canScroll = { canScroll },
        flingAnimationSpec = null
    )
    var lastCellIndex by rememberGridSize()

    val pinchState = rememberPinchZoomGridState(
        cellsList = cellsList,
        initialCellsIndex = lastCellIndex
    )

    val isScrolling = remember { mutableStateOf(false) }

    LaunchedEffect(pinchState.isZooming) {
        withContext(Dispatchers.IO) {
            canScroll = !pinchState.isZooming
            lastCellIndex = cellsList.indexOf(pinchState.currentCells)
        }
    }

    LaunchedEffect(selectionState.value) {
        toggleNavbar(!selectionState.value)
    }

    Box {
        val scaffoldModifier = remember { Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) }
        Scaffold(
            modifier = scaffoldModifier,
            topBar = {
                TopAppBar(
                    title = {
                        TwoLinedDateToolbarTitle(
                            albumName = "Search by Hue",
                            dateHeader = mediaState.value.dateHeader
                        )
                    },
                    navigationIcon = {
                        NavigationButton(
                            albumId = -1L,
                            target = "hue=$currentColor",
                            navigateUp = navigateUp,
                            clearSelection = {
                                selectionState.value = false
                                selectedMedia.clear()
                            },
                            selectionState = selectionState,
                            alwaysGoBack = false,
                        )
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (classifiedCount.value == 0 && !isRunning.value) {
                            NoHueInfo(onClick = startClassification)
                        }

                        if (classifiedCount.value != 0 || isRunning.value) {
                            ScannerButton(
                                isRunning = isRunning.value,
                                indicatorCounter = progress.value,
                                contentColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onLongClick = {
                                            if (isRunning.value) stopClassification()
                                        },
                                        onClick = {
                                            if (!isRunning.value) startClassification()
                                        }
                                    )
                            )
                        }

                        if (classifiedCount.value != 0 && !isRunning.value) {
                            LibrarySmallItem(
                                title = "Delete all hue info",
                                icon = Icons.Default.Delete,
                                contentColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clickable(onClick = deleteClassifications)
                            )
                        }
                    }

                    PinchZoomGridLayout(pinchState) {
                        if (!isRunning.value) {
                            MediaGridView(
                                mediaState = mediaState,
                                metadataState = metadataState,
                                allowSelection = true,
                                canScroll = canScroll,
                                selectionState = selectionState,
                                selectedMedia = selectedMedia,
                                toggleSelection = {
                                    toggleSelection(mediaState.value, it)
                                },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedContentScope = animatedContentScope,
                                isScrolling = isScrolling,
                                emptyContent = { EmptyMedia() },
                                allowHeaders = false,
                                enableStickyHeaders = false
                            ) {
                                navigate(Screen.MediaViewScreen.route + "?mediaId=${it.id}&hue=${currentColor.value.toArgb()}")
                            }
                        } else {
                            EmptyMedia(title = "Kindly wait for the scan to complete...")
                        }
                    }
                }

                SupportiveLayout(
                    isSupportingPanel = false,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp)
                ) {
                    HueBar(
                        modifier = Modifier.padding(WindowInsets.horizontalSystemGesturesPadding()),
                        currentColor = currentColor.value,
                        isSupportingPanel = false,
                        enabled = true
                    ) { hue ->
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(currentColor.value.toArgb(), hsv)
                        hsv[0] = hue
                        val newColor = Color(
                            android.graphics.Color.HSVToColor(
                                (currentColor.value.alpha * 255).toInt(),
                                hsv
                            )
                        )
                        setColor(newColor)
                    }
                }
            }
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomEnd),
            visible = true,
            enter = enterAnimation,
            exit = exitAnimation
        ) {
            val selectedMediaList =
                mediaState.value.media.selectedMedia(selectedSet = selectedMedia)
            SelectionSheet(
                modifier = Modifier
                    .align(Alignment.BottomEnd),
                selectedMedia = selectedMediaList,
                selectionState = selectionState,
                albumsState = albumsState,
                handler = handler
            )
        }
    }
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