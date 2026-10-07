/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.albums.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.LocalMediaImageRenderer
import com.dot.gallery.core.presentation.components.OptionItem
import com.dot.gallery.core.presentation.components.OptionSheet
import com.dot.gallery.core.ui.util.rememberAppBottomSheetState
import com.dot.gallery.core.util.rememberFeedbackManager
import com.dot.gallery.domain.model.Album
import com.dot.gallery.ui.theme.Shapes
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.albums.generated.resources.Res
import com.dot.gallery.feature.albums.generated.resources.browse_subfolders
import com.dot.gallery.feature.albums.generated.resources.item_count
import com.dot.gallery.feature.albums.generated.resources.merge_subfolders
import com.dot.gallery.feature.albums.generated.resources.s_items
import com.dot.gallery.feature.albums.generated.resources.show_combined_timeline
import com.dot.gallery.feature.albums.generated.resources.unmerge_subfolders
import com.dot.gallery.feature.albums.generated.resources.unpin

/**
 * Pinned albums shown as a Material3 multi-browse carousel. Pure-Compose port
 * of the original Material `CarouselLayoutManager` + RecyclerView
 * implementation — masking, label fade-out on item shrink and the long-click
 * option sheet are preserved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarouselPinnedAlbums(
    modifier: Modifier = Modifier,
    albumList: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
    mergedSubfolderIds: Set<Long> = emptySet(),
    subGalleryIds: Set<Long> = emptySet(),
    onToggleMergeSubfolders: ((Album) -> Unit)? = null,
    onToggleMergedSubfolderDisplayMode: ((Album) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val appBottomSheetState = rememberAppBottomSheetState()
    var currentAlbum: Album? by remember { mutableStateOf(null) }
    val selectedAlbum = currentAlbum?.let { selected ->
        albumList.firstOrNull { it.id == selected.id } ?: selected
    }
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    // Match the legacy adapter item width: screen width / 2.75.
    val preferredItemWidth = remember(density, windowInfo) {
        with(density) { windowInfo.containerSize.width.toDp() / 2.75f }
    }
    val carouselState = rememberCarouselState(itemCount = { albumList.size })

    HorizontalMultiBrowseCarousel(
        state = carouselState,
        modifier = modifier
            .fillMaxWidth()
            .height(196.dp),
        preferredItemWidth = preferredItemWidth,
        itemSpacing = 0.dp,
        contentPadding = PaddingValues(0.dp)
    ) { index ->
        val album = albumList[index]
        val feedbackManager = rememberFeedbackManager()
        val interactionSource = remember { MutableInteractionSource() }
        // Legacy behavior: the label/summary slide and fade out as the item gets
        // masked down to its small edge form (maskRect.left grows from 0).
        val maskLeft = carouselItemDrawInfo.maskRect.left
        val labelAlpha = (1f - maskLeft / 80f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .maskClip(Shapes.large)
        ) {
            when (albumThumbnailPresentation(album.isLocked)) {
                AlbumThumbnailPresentation.LOCKED_PLACEHOLDER -> LockedAlbumThumbnail(
                    modifier = Modifier
                        .fillMaxSize()
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current,
                            onClick = { onAlbumClick(album) },
                            onLongClick = {
                                feedbackManager.vibrate()
                                currentAlbum = album
                                scope.launch { appBottomSheetState.show() }
                            }
                        ),
                    iconModifier = Modifier.size(36.dp),
                    shape = Shapes.large
                )
                AlbumThumbnailPresentation.MEDIA -> {
                    val renderer = LocalMediaImageRenderer.current
                    renderer.RenderImage(
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = LocalIndication.current,
                                onClick = { onAlbumClick(album) },
                                onLongClick = {
                                    feedbackManager.vibrate()
                                    currentAlbum = album
                                    scope.launch { appBottomSheetState.show() }
                                }
                            ),
                        model = album.uri,
                        contentDescription = album.label,
                        contentScale = ContentScale.Crop,
                        signature = album
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.surface
                        )
                    )
                    .graphicsLayer {
                        alpha = labelAlpha
                        translationX = maskLeft
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = album.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = pluralStringResource(
                        Res.plurals.item_count,
                        album.count.toInt(),
                        album.count
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }

    val unpinTitle = stringResource(Res.string.unpin)
    val mergeTitle = stringResource(
        if (selectedAlbum?.id in mergedSubfolderIds) Res.string.unmerge_subfolders
        else Res.string.merge_subfolders
    )
    val layoutTitle = stringResource(
        if (selectedAlbum?.id in subGalleryIds) Res.string.show_combined_timeline
        else Res.string.browse_subfolders
    )
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val onTertiaryContainer = MaterialTheme.colorScheme.onTertiaryContainer
    val optionList = remember(
        selectedAlbum,
        mergedSubfolderIds,
        subGalleryIds,
        onToggleMergeSubfolders,
        onToggleMergedSubfolderDisplayMode,
        mergeTitle,
        layoutTitle
    ) {
        mutableListOf(
            OptionItem(
                text = unpinTitle,
                containerColor = tertiaryContainer,
                contentColor = onTertiaryContainer,
                onClick = {
                    scope.launch {
                        appBottomSheetState.hide()
                        currentAlbum?.let {
                            onAlbumLongClick(it)
                            currentAlbum = null
                        }
                    }
                }
            )
        ).apply {
            if (onToggleMergeSubfolders != null) {
                add(
                    OptionItem(
                        icon = Icons.Outlined.AccountTree,
                        text = mergeTitle,
                        onClick = {
                            scope.launch {
                                appBottomSheetState.hide()
                                currentAlbum?.let(onToggleMergeSubfolders)
                                currentAlbum = null
                            }
                        }
                    )
                )
            }
            if (
                selectedAlbum?.id in mergedSubfolderIds &&
                onToggleMergedSubfolderDisplayMode != null
            ) {
                add(
                    OptionItem(
                        icon = Icons.Outlined.AccountTree,
                        text = layoutTitle,
                        onClick = {
                            scope.launch {
                                appBottomSheetState.hide()
                                currentAlbum?.let(onToggleMergedSubfolderDisplayMode)
                                currentAlbum = null
                            }
                        }
                    )
                )
            }
        }.toMutableStateList()
    }

    OptionSheet(
        state = appBottomSheetState,
        optionList = arrayOf(optionList),
        headerContent = {
            selectedAlbum?.let { album ->
                when (albumThumbnailPresentation(album.isLocked)) {
                    AlbumThumbnailPresentation.LOCKED_PLACEHOLDER -> LockedAlbumThumbnail(
                        modifier = Modifier.size(98.dp),
                        iconModifier = Modifier.size(36.dp)
                    )
                    AlbumThumbnailPresentation.MEDIA -> {
                        val renderer = LocalMediaImageRenderer.current
                        renderer.RenderImage(
                            modifier = Modifier
                                .size(98.dp)
                                .clip(Shapes.large),
                            contentScale = ContentScale.Crop,
                            model = album.uri,
                            contentDescription = album.label,
                            signature = album
                        )
                    }
                }
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontStyle = MaterialTheme.typography.titleLarge.fontStyle,
                                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                letterSpacing = MaterialTheme.typography.titleLarge.letterSpacing
                            )
                        ) {
                            append(album.label)
                        }
                        append("\n")
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = MaterialTheme.typography.bodyMedium.fontStyle,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                letterSpacing = MaterialTheme.typography.bodyMedium.letterSpacing
                            )
                        ) {
                            append(stringResource(Res.string.s_items, album.count))
                        }
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                )
            }
        }
    )
}
