/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.signature.MediaStoreSignature
import com.dot.gallery.R
import com.dot.gallery.core.Constants.PERMISSIONS
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.presentation.components.Error
import com.dot.gallery.core.presentation.components.NavigationActions
import com.dot.gallery.core.presentation.components.NavigationButton
import com.dot.gallery.core.presentation.components.media.MediaComponent
import com.dot.gallery.core.presentation.components.util.StickyHeaderGrid
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaItem
import com.dot.gallery.feature_node.domain.model.isHeaderKey
import com.dot.gallery.feature_node.presentation.timeline.components.StickyHeader
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.feature_node.presentation.util.vibrate
import com.dot.gallery.ui.theme.Dimens
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(
    ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class
)
@Composable
fun MediaScreen(
    paddingValues: PaddingValues,
    albumId: Long = -1L,
    target: String? = null,
    albumName: String,
    retrieveMedia: (() -> Unit)? = null,
    mediaState: MutableState<MediaState>,
    selectionState: MutableState<Boolean>,
    selectedMedia: SnapshotStateList<Media>,
    toggleSelection: (Int) -> Unit,
    showMonthlyHeader: Boolean = false,
    alwaysGoBack: Boolean = true,
    NavActions: @Composable (RowScope.(expandedDropDown: MutableState<Boolean>, result: ActivityResultLauncher<IntentSenderRequest>) -> Unit),
    EmptyComponent: @Composable () -> Unit,
    OverGrid: @Composable (() -> Unit)? = null,
    navigate: (route: String) -> Unit,
    navigateUp: () -> Unit,
    onActivityResult: (result: ActivityResult) -> Unit,
) {

    /** STRING BLOCK **/
    val stringToday = stringResource(id = R.string.header_today)
    val stringYesterday = stringResource(id = R.string.header_yesterday)
    val requestPermission = stringResource(R.string.request_permissions)
    /** ************ **/

    /** Permission Handling BLOCK **/
    val mediaPermissions =
        rememberMultiplePermissionsState(PERMISSIONS) { retrieveMedia?.invoke() }
    /** Trigger retrieveMedia after permission is granted */
    if (!mediaPermissions.allPermissionsGranted) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = mediaPermissions::launchMultiplePermissionRequest) {
                Text(text = requestPermission)
            }
        }
    }
    /** ************ **/
    else {
        val scrollBehavior =
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

        /** STATES BLOCK **/
        val gridState = rememberLazyGridState()
        val state by mediaState
        val mappedData = if (showMonthlyHeader) state.mappedMediaWithMonthly else state.mappedMedia
        /** ************ **/

        /** Glide Preloading **/
        val preloadingData = rememberGlidePreloadingData(
            data = state.media,
            preloadImageSize = Size(100f, 100f)
        ) { media: Media, requestBuilder: RequestBuilder<Drawable> ->
            requestBuilder
                .signature(MediaStoreSignature(media.mimeType, media.timestamp, media.orientation))
                .load(media.uri)
        }
        /** ************ **/

        /** Selection state handling **/
        val clearSelection = remember {
            {
                selectionState.value = false
                selectedMedia.clear()
            }
        }

        BackHandler(enabled = selectionState.value, onBack = clearSelection)
        /** ************  **/

        /**
         * Remember last known header item
         */
        val stickyHeaderLastItem = remember { mutableStateOf<String?>(null) }

        val stickyHeaderItem by remember(state.media) {
            derivedStateOf {
                var firstIndex = gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
                var item = firstIndex?.let(mappedData::getOrNull)
                if (item != null && item.key.contains("big")) {
                    firstIndex = firstIndex!! + 1
                    item = firstIndex.let(mappedData::getOrNull)
                }
                stickyHeaderLastItem.apply {
                    if (item != null && item is MediaItem.Header) {
                        value = item.text
                            .replace("Today", stringToday)
                            .replace("Yesterday", stringYesterday)
                    }
                }.value
            }
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            val toolbarTitle = if (selectionState.value) stringResource(
                                R.string.selected_s,
                                selectedMedia.size
                            ) else albumName
                            Text(
                                text = toolbarTitle,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                            if (state.dateHeader.isNotEmpty()) {
                                Text(
                                    modifier = Modifier,
                                    text = state.dateHeader.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        NavigationButton(
                            albumId = albumId,
                            navigateUp = navigateUp,
                            clearSelection = clearSelection,
                            selectionState = selectionState,
                            alwaysGoBack = alwaysGoBack,
                        )
                    },
                    actions = {
                        NavigationActions(
                            actions = NavActions,
                            onActivityResult = onActivityResult
                        )
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { it ->
            StickyHeaderGrid(
                modifier = Modifier.fillMaxSize(),
                lazyState = gridState,
                headerMatcher = { item -> item.key.isHeaderKey },
                stickyHeader = {
                    if (state.media.isNotEmpty()) {
                        stickyHeaderItem?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                // 3.dp is the elevation the LargeTopAppBar use
                                                MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .padding(horizontal = 16.dp, vertical = 24.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            ) {
                LazyVerticalGrid(
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(Dimens.Photo()),
                    contentPadding = PaddingValues(
                        top = it.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    if (OverGrid != null) {
                        item(
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            OverGrid.invoke()
                        }
                    }
                    items(
                        items = mappedData,
                        key = { it.key },
                        contentType = { it.key.startsWith("media_") },
                        span = { item ->
                            GridItemSpan(if (item.key.isHeaderKey) maxLineSpan else 1)
                        }
                    ) { item ->
                        when (item) {
                            is MediaItem.Header -> {
                                val isChecked = rememberSaveable { mutableStateOf(false) }
                                LaunchedEffect(selectionState.value) {
                                    // Uncheck if selectionState is set to false
                                    isChecked.value = isChecked.value && selectionState.value
                                }
                                LaunchedEffect(selectedMedia.size) {
                                    // Partial check of media items should not check the header
                                    isChecked.value = selectedMedia.containsAll(item.data)
                                }
                                val title = item.text
                                    .replace("Today", stringToday)
                                    .replace("Yesterday", stringYesterday)
                                StickyHeader(
                                    date = title,
                                    showAsBig = item.key.contains("big"),
                                    isCheckVisible = selectionState,
                                    isChecked = isChecked
                                ) {
                                    isChecked.value = !isChecked.value
                                    if (isChecked.value) {
                                        val toAdd = item.data.toMutableList().apply {
                                            // Avoid media from being added twice to selection
                                            removeIf { selectedMedia.contains(it) }
                                        }
                                        selectedMedia.addAll(toAdd)
                                    } else selectedMedia.removeAll(item.data)
                                    selectionState.value = selectedMedia.isNotEmpty()
                                }
                            }

                            is MediaItem.MediaViewItem -> {
                                val (media, preloadRequestBuilder) = preloadingData[state.media.indexOf(
                                    item.media
                                )]
                                val view = LocalView.current
                                MediaComponent(
                                    media = media,
                                    selectionState = selectionState,
                                    selectedMedia = selectedMedia,
                                    preloadRequestBuilder = preloadRequestBuilder,
                                    onItemLongClick = {
                                        view.vibrate()
                                        toggleSelection(state.media.indexOf(it))
                                    },
                                    onItemClick = {
                                        if (selectionState.value) {
                                            view.vibrate()
                                            toggleSelection(state.media.indexOf(it))
                                        } else {
                                            val albumRoute = "albumId=$albumId"
                                            val targetRoute = "target=$target"
                                            val param = if (target != null) targetRoute else albumRoute
                                            navigate(Screen.MediaViewScreen.route + "?mediaId=${it.id}&$param")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        /** Error State Handling Block **/
        if (state.error.isNotEmpty())
            Error(errorMessage = state.error)
        else if (state.media.isEmpty())
            EmptyComponent.invoke()
        /** ************ **/
    }
}