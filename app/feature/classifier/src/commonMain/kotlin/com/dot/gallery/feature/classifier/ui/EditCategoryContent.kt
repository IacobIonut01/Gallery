/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.classifier.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Constants.cellsList
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.core.presentation.components.EmptyMedia
import com.dot.gallery.core.presentation.components.GridPinchZoomLayout
import com.dot.gallery.core.presentation.components.MediaGridView
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.core.presentation.components.rememberGridPinchZoomState
import com.dot.gallery.core.presentation.util.LocalHazeState
import com.dot.gallery.core.presentation.util.hazeEffectScaled
import com.dot.gallery.data.model.MediaMetadataState
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.domain.model.UriMedia
import dev.chrisbanes.haze.blur.LocalHazeBlurStyle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.classifier.generated.resources.Res
import com.dot.gallery.feature.classifier.generated.resources.action_cancel
import com.dot.gallery.feature.classifier.generated.resources.action_delete
import com.dot.gallery.feature.classifier.generated.resources.best_match
import com.dot.gallery.feature.classifier.generated.resources.category_name_hint
import com.dot.gallery.feature.classifier.generated.resources.delete_category_confirmation
import com.dot.gallery.feature.classifier.generated.resources.delete_category_title
import com.dot.gallery.feature.classifier.generated.resources.no_matching_photos
import com.dot.gallery.feature.classifier.generated.resources.save


/**
 * Edit-category form with a live matching-preview bottom sheet.
 *
 * @param lastCellIndex persisted media-grid cell index for the preview sheet
 * (platform store — app-side).
 */
@OptIn(
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun EditCategoryContent(
    state: EditCategoryState,
    actions: EditCategoryActions,
    previewMediaState: MediaState<UriMedia>,
    metadataState: State<MediaMetadataState>,
    isScrolling: MutableState<Boolean>,
    lastCellIndex: MutableState<Int>,
    paddingValues: PaddingValues,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.categoryId) {
        actions.onLoadCategory(state.categoryId)
    }

    val scope = rememberCoroutineScope()
    var lastCellIndexState by lastCellIndex
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden
        )
    )

    val density = LocalDensity.current
    val dragHandleAlpha by remember {
        derivedStateOf {
            val offset =
                runCatching { scaffoldState.bottomSheetState.requireOffset() }.getOrElse { Float.MAX_VALUE }
            val fadeThreshold = with(density) { 200.dp.toPx() }
            (offset / fadeThreshold).coerceIn(0f, 1f)
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
        flingAnimationSpec = null
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.delete_category_title)) },
            text = { Text(stringResource(Res.string.delete_category_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        actions.onDelete(actions.onNavigateBack)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(Res.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    }

    Box(
        modifier = Modifier.padding(
            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
        )
    ) {
        if (state.isLoading && state.category == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetPeekHeight = 0.dp,
                sheetDragHandle = { DragHandle(alpha = dragHandleAlpha) },
                sheetContent = {
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.best_match),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    val sheetDpCacheWindow = LazyLayoutCacheWindow(ahead = 200.dp, behind = 100.dp)
                    val sheetPinchState = rememberGridPinchZoomState(
                        cellsList = cellsList,
                        initialCellsIndex = lastCellIndexState,
                        gridState = rememberLazyGridState(cacheWindow = sheetDpCacheWindow)
                    )
                    GridPinchZoomLayout(state = sheetPinchState) {
                        MediaGridView(
                            mediaState = remember(previewMediaState) {
                                mutableStateOf(previewMediaState.copy(isLoading = false))
                            },
                            metadataState = metadataState,
                            allowSelection = false,
                            showSearchBar = false,
                            enableStickyHeaders = false,
                            paddingValues = PaddingValues(
                                bottom = 128.dp
                            ),
                            canScroll = true,
                            allowHeaders = false,

                            isScrolling = isScrolling,
                            emptyContent = {
                                EmptyMedia(
                                    title = stringResource(Res.string.no_matching_photos)
                                )
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope
                        ) { media ->
                            actions.onOpenPreviewMedia(media.id)
                        }
                    }
                }
            ) {
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        LargeTopAppBar(
                            modifier = Modifier.hazeEffectScaled(
                                state = LocalHazeState.current,
                                style = LocalHazeBlurStyle.current
                            ),
                            title = {
                                CategoryNameInput(
                                    value = state.categoryName,
                                    onValueChange = actions.onNameChange,
                                    placeholder = stringResource(Res.string.category_name_hint)
                                )
                            },
                            navigationIcon = {
                                NavigationBackButton(forcedAction = actions.onNavigateBack)
                            },
                            actions = {
                                IconButton(onClick = { showDeleteDialog = true }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = stringResource(Res.string.action_delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            scrollBehavior = scrollBehavior,
                            colors = TopAppBarDefaults.topAppBarColors(
                                scrolledContainerColor = MaterialTheme.colorScheme.surface,
                            ),
                        )
                    },
                    floatingActionButton = {
                        AnimatedVisibility(
                            visible = state.isValid && !state.isSaving,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    keyboardController?.hide()
                                    actions.onSave(actions.onNavigateBack)
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = stringResource(Res.string.save),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(top = innerPadding.calculateTopPadding())
                            .padding(
                                bottom = paddingValues.calculateBottomPadding() + 128.dp
                            )
                            .verticalScroll(rememberScrollState())
                    ) {
                        CategoryInputControls(
                            searchTerms = state.searchTerms,
                            onSearchTermsChange = actions.onSearchTermsChange,
                            threshold = state.threshold,
                            onThresholdChange = actions.onThresholdChange,
                            isLoading = state.isLoading,
                            previewCount = state.previewCount,
                            onMatchingPhotosClick = {
                                if (state.previewCount > 0) {
                                    scope.launch { scaffoldState.bottomSheetState.expand() }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
