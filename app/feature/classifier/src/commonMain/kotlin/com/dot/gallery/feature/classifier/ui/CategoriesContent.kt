/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.classifier.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Constants.albumCellsList
import com.dot.gallery.core.presentation.components.Error
import com.dot.gallery.core.presentation.components.GridPinchZoomLayout
import com.dot.gallery.core.presentation.components.LoadingMediaContent
import com.dot.gallery.core.presentation.components.LocalMediaImageRenderer
import com.dot.gallery.core.presentation.components.MediaContentState
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.core.presentation.components.TwoLinedDateToolbarTitle
import com.dot.gallery.core.presentation.components.mediaContentState
import com.dot.gallery.core.presentation.components.rememberGridPinchZoomState
import com.dot.gallery.core.presentation.util.LocalHazeState
import com.dot.gallery.core.presentation.util.categorySharedElement
import com.dot.gallery.core.presentation.util.hazeEffectScaled
import com.dot.gallery.core.ui.components.dashedBorder
import com.dot.gallery.core.ui.settings.rememberPreference
import com.dot.gallery.core.ui.util.supportsLoadingShimmer
import com.dot.gallery.core.util.rememberFeedbackManager
import com.dot.gallery.data.dao.CategoryWithMediaCount
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.domain.settings.SettingsKeys
import com.dot.gallery.domain.util.getUri
import com.dot.gallery.ui.theme.BlackScrim
import com.dot.gallery.ui.theme.WhiterBlackScrim
import com.dot.gallery.ui.theme.isDarkTheme
import dev.chrisbanes.haze.blur.LocalHazeBlurStyle
import dev.chrisbanes.haze.hazeSource
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.classifier.generated.resources.Res
import com.dot.gallery.feature.classifier.generated.resources.add_category
import com.dot.gallery.feature.classifier.generated.resources.categories
import com.dot.gallery.feature.classifier.generated.resources.categories_settings
import com.dot.gallery.feature.classifier.generated.resources.categories_settings_subtitle
import com.dot.gallery.feature.classifier.generated.resources.category_media_count
import com.dot.gallery.feature.classifier.generated.resources.classified_media
import com.dot.gallery.feature.classifier.generated.resources.no_categories_found


/**
 * Categories grid — categories plus a settings row, with pinch-zoom cell sizing.
 *
 * @param lastCellIndex persisted album-grid cell index (platform store — app-side).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CategoriesContent(
    state: CategoriesState,
    actions: CategoriesActions,
    lastCellIndex: MutableState<Int>,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    var lastCellIndexState by lastCellIndex
    var canScroll by rememberSaveable { mutableStateOf(true) }

    val pinchState = rememberGridPinchZoomState(
        cellsList = albumCellsList,
        initialCellsIndex = lastCellIndexState
    )

    LaunchedEffect(pinchState.isZooming) {
        canScroll = !pinchState.isZooming
        lastCellIndexState = albumCellsList.indexOf(pinchState.currentCells)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
        canScroll = { canScroll },
        flingAnimationSpec = null
    )

    val categoriesSettingsTitle = stringResource(Res.string.categories_settings)
    val categoriesSettingsSubtitle = stringResource(Res.string.categories_settings_subtitle)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                modifier = Modifier.hazeEffectScaled(
                    state = LocalHazeState.current,
                    style = LocalHazeBlurStyle.current
                ),
                title = {
                    TwoLinedDateToolbarTitle(
                        albumName = stringResource(Res.string.categories),
                        dateHeader = stringResource(Res.string.classified_media, state.distinctMediaCount)
                    )
                },
                navigationIcon = {
                    NavigationBackButton()
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = actions.onCreateCategory,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.add_category),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { paddingValues ->
        // hazeSource stays attached in every content state so the bars keep their frosted
        // effect when the grid is empty/loading/error (no captured areas = effect draws nothing).
        Box(modifier = Modifier.fillMaxSize().hazeSource(LocalHazeState.current)) {
        when (mediaContentState(
            isLoading = state.mediaState.isLoading,
            error = state.mediaState.error,
            isEmpty = state.categoriesWithCount.isEmpty(),
        )) {
            MediaContentState.ERROR -> Error(
                modifier = Modifier.padding(paddingValues),
                errorMessage = state.mediaState.error,
            )
            MediaContentState.LOADING -> LoadingMediaContent(
                grid = albumCellsList.size - lastCellIndexState,
                canShimmer = supportsLoadingShimmer,
                modifier = Modifier.padding(paddingValues),
            )
            MediaContentState.EMPTY,
            MediaContentState.CONTENT -> GridPinchZoomLayout(
            state = pinchState,
            indicatorTopPadding = paddingValues.calculateTopPadding() + 16.dp,
        ) {
            LazyVerticalGrid(
                state = gridState,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),
                columns = gridCells,
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + 128.dp
                ),
                userScrollEnabled = canScroll,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Settings button at the top
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "categories_settings"
                ) {
                    ListItem(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable { actions.onOpenSettings() },
                        headlineContent = {
                            Text(
                                text = categoriesSettingsTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        supportingContent = {
                            Text(
                                text = categoriesSettingsSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }

                // Category grid items
                items(
                    items = state.categoriesWithCount,
                    key = { it.id }
                ) { categoryWithCount ->
                    val thumbnailMedia = remember(categoryWithCount.thumbnailMediaId, state.mediaState.media) {
                        categoryWithCount.thumbnailMediaId?.let { thumbId ->
                            state.mediaState.media.find { it.id == thumbId }
                        }
                    }

                    with(sharedTransitionScope) {
                        CategoryGridItem(
                            categoryWithCount = categoryWithCount,
                            media = thumbnailMedia,
                            onClick = {
                                actions.onOpenCategory(categoryWithCount.id)
                            },
                            onLongClick = {
                                actions.onEditCategory(categoryWithCount.id)
                            },
                            modifier = Modifier
                                .pinchItem(key = categoryWithCount.id.toString())
                                .animateItem()
                                .categorySharedElement(
                                    categoryId = categoryWithCount.id,
                                    animatedVisibilityScope = animatedContentScope
                                )
                        )
                    }
                }

                // Empty state
                if (state.categoriesWithCount.isEmpty()) {
                    item(
                        span = { GridItemSpan(maxLineSpan) },
                        key = "empty_state"
                    ) {
                        val brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary,
                            )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .dashedBorder(
                                    brush = brush,
                                    shape = RoundedCornerShape(24.dp),
                                    gapLength = 8.dp
                                )
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { actions.onCreateCategory() }
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
                                text = stringResource(Res.string.no_categories_found),
                                style = MaterialTheme.typography.titleMedium.copy(brush = brush),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        }
        }
    }
}

@Composable
private fun CategoryGridItem(
    categoryWithCount: CategoryWithMediaCount,
    media: UriMedia?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageRenderer = LocalMediaImageRenderer.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed.value) 32.dp else 24.dp,
        label = "cornerRadius"
    )
    val feedbackManager = rememberFeedbackManager()
    val isDarkTheme = isDarkTheme()
    val allowBlur by rememberPreference(SettingsKeys.allowBlur)
    val followTheme = remember(allowBlur) { !allowBlur }
    val gradientColor by animateColorAsState(
        if (followTheme) {
            if (isDarkTheme) BlackScrim else WhiterBlackScrim
        } else BlackScrim,
    )

    Box(
        modifier = modifier
            .aspectRatio(164f / 256f)
            .clip(RoundedCornerShape(cornerRadius))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = {
                    feedbackManager.vibrate()
                    onLongClick()
                }
            )
    ) {
        if (media != null) {
            imageRenderer.RenderImage(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                model = media.getUri(),
                contentDescription = categoryWithCount.name,
                signature = media
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ImageSearch,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            gradientColor
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = categoryWithCount.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            if (categoryWithCount.mediaCount > 0) {
                Text(
                    text = stringResource(Res.string.category_media_count, categoryWithCount.mediaCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
