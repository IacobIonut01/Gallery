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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.signature.MediaStoreSignature
import com.dot.gallery.R
import com.dot.gallery.core.Constants.PERMISSIONS
import com.dot.gallery.core.presentation.components.Error
import com.dot.gallery.core.presentation.components.NavigationActions
import com.dot.gallery.core.presentation.components.NavigationButton
import com.dot.gallery.core.presentation.components.media.MediaComponent
import com.dot.gallery.core.presentation.components.util.StickyHeaderGrid
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaItem
import com.dot.gallery.feature_node.domain.model.isHeaderKey
import com.dot.gallery.feature_node.presentation.timeline.components.StickyHeader
import com.dot.gallery.feature_node.presentation.util.DateExt
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.feature_node.presentation.util.getDateExt
import com.dot.gallery.feature_node.presentation.util.getDateHeader
import com.dot.gallery.feature_node.presentation.util.getMonth
import com.dot.gallery.ui.theme.Dimens
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(
    ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class
)
@Composable
fun MediaScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    albumId: Long = -1L,
    albumName: String,
    viewModel: MediaViewModel,
    showMonthlyHeader: Boolean = false,
    alwaysGoBack: Boolean = true,
    NavActions: @Composable RowScope.(
        expandedDropDown: MutableState<Boolean>,
        selectedMedia: SnapshotStateList<Media>,
        selectionState: MutableState<Boolean>,
        result: ActivityResultLauncher<IntentSenderRequest>
    ) -> Unit,
    EmptyComponent: @Composable () -> Unit,
    OverGrid: (@Composable () -> Unit)? = null,
    onActivityResult: (
        selectedMedia: SnapshotStateList<Media>,
        selectionState: MutableState<Boolean>,
        result: ActivityResult
    ) -> Unit
) {

    /** STRING BLOCK **/
    val stringToday = stringResource(id = R.string.header_today)
    val stringYesterday = stringResource(id = R.string.header_yesterday)
    val requestPermission = stringResource(R.string.request_permissions)
    /** ************ **/

    /** Permission Handling BLOCK **/
    val mediaPermissions =
        rememberMultiplePermissionsState(PERMISSIONS) { viewModel.launchInPhotosScreen() }
    /** Trigger viewModel launch after permission is granted */
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
        val state by remember { viewModel.photoState }
        val selectionState = remember { viewModel.multiSelectState }
        val selectedMedia = remember { viewModel.selectedPhotoState }
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

        BackHandler(enabled = selectionState.value) { clearSelection() }
        /** ************  **/

        /**
         * This block requires recomputing on state change
         */
        val sortedAscendingMedia: List<Media> = remember(state.media) {
            state.media.sortedBy { it.timestamp }
        }
        val startDate: DateExt? = remember(state.media) {
            try {
                sortedAscendingMedia.first().timestamp.getDateExt()
            } catch (e: NoSuchElementException) {
                null
            }
        }
        val endDate: DateExt? = remember(state.media) {
            try {
                sortedAscendingMedia.last().timestamp.getDateExt()
            } catch (e: NoSuchElementException) {
                null
            }
        }
        val subtitle: String = remember(state.media) {
            if (albumId != -1L && startDate != null && endDate != null)
                getDateHeader(startDate, endDate)
            else ""
        }
        val mappedData = remember { ArrayList<MediaItem>() }
        val monthHeaderList = remember { ArrayList<String>() }
        remember(state.media) {
            mappedData.clear()
            if (showMonthlyHeader) {
                monthHeaderList.clear()
            }
            state.media.groupBy {
                it.timestamp.getDate(
                    stringToday = stringToday,
                    stringYesterday = stringYesterday
                )
            }.forEach { (date, data) ->
                if (showMonthlyHeader) {
                    val month = getMonth(date)
                    if (month.isNotEmpty() && !monthHeaderList.contains(month)) {
                        monthHeaderList.add(month)
                        mappedData.add(MediaItem.Header("header_big_$month", month))
                    }
                }
                mappedData.add(MediaItem.Header("header_$date", date))
                for (media in data) {
                    mappedData.add(MediaItem.MediaViewItem.Loaded("media_${media.id}", media))
                }
            }
            true
        }
        /** ************ **/

        /**
         * Remember last known header item
         */
        val stickyHeaderLastItem = remember { mutableStateOf<String?>(null) }

        val stickyHeaderItem by remember(state.media) {
            derivedStateOf {
                val firstIndex = gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
                val item = firstIndex?.let(mappedData::getOrNull)
                stickyHeaderLastItem.apply {
                    if (item != null && item is MediaItem.Header) {
                        value = item.key.replace("header_", "")
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
                            if (subtitle.isNotEmpty()) {
                                Text(
                                    modifier = Modifier,
                                    text = subtitle.uppercase(),
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
                            navController = navController,
                            clearSelection = clearSelection,
                            selectionState = selectionState,
                            alwaysGoBack = alwaysGoBack,
                        )
                    },
                    actions = {
                        NavigationActions(
                            selectedMedia = selectedMedia,
                            selectionState = selectionState,
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
                        span = { item ->
                            GridItemSpan(if (item.key.isHeaderKey) maxLineSpan else 1)
                        }
                    ) { item ->
                        when (item) {
                            is MediaItem.Header -> StickyHeader(
                                date = item.text,
                                item.key.contains("big")
                            )

                            is MediaItem.MediaViewItem -> {
                                val (media, preloadRequestBuilder) = preloadingData[state.media.indexOf(
                                    item.media
                                )]
                                MediaComponent(
                                    media = media,
                                    selectionState = selectionState,
                                    selectedMedia = selectedMedia,
                                    preloadRequestBuilder = preloadRequestBuilder,
                                    onItemLongClick = {
                                        viewModel.toggleSelection(state.media.indexOf(it))
                                    },
                                    onItemClick = {
                                        if (selectionState.value) {
                                            viewModel.toggleSelection(state.media.indexOf(it))
                                        } else {
                                            val albumRoute = "albumId=${viewModel.albumId}"
                                            val targetRoute = "target=${viewModel.target}"
                                            val param = if (viewModel.target != null) targetRoute else albumRoute
                                            navController.navigate(
                                                Screen.MediaViewScreen.route + "?mediaId=${it.id}&$param"
                                            )
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