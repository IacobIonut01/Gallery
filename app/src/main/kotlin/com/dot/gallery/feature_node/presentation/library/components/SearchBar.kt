/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.library.components

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isContainer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.signature.MediaStoreSignature
import com.dot.gallery.R
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Settings.Search.rememberSearchHistory
import com.dot.gallery.core.presentation.components.StickyHeader
import com.dot.gallery.core.presentation.components.media.MediaComponent
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaItem
import com.dot.gallery.feature_node.domain.model.isHeaderKey
import com.dot.gallery.feature_node.presentation.library.SearchViewModel
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSearchBar(
    bottomPadding: Dp,
    selectionState: MutableState<Boolean>? = null,
    navigate: (String) -> Unit,
    toggleNavbar: (Boolean) -> Unit,
    menuItems: @Composable (RowScope.() -> Unit)? = null,
) {
    var historySet by rememberSearchHistory()
    val stringToday = stringResource(id = R.string.header_today)
    val stringYesterday = stringResource(id = R.string.header_yesterday)
    val vm = hiltViewModel<SearchViewModel>()
    val gridState = rememberLazyGridState()
    var query by rememberSaveable { mutableStateOf("") }
    val state by vm.mediaState
    var activeState by rememberSaveable {
        mutableStateOf(false)
    }
    val dismissSearchBar = remember {
        {
            activeState = false
            query = ""
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (selectionState != null && selectionState.value) 0.6f else 1f,
        label = "alpha"
    )
    LaunchedEffect(LocalConfiguration.current, activeState) {
        if (selectionState == null || !selectionState.value)
            toggleNavbar(!activeState)
    }

    LaunchedEffect(query) {
        snapshotFlow { query }.collect {
            vm.queryMedia(it)
        }
    }

    /** Glide Preloading **/
    val preloadingData = rememberGlidePreloadingData(
        data = state.media,
        preloadImageSize = Size(50f, 50f)
    ) { media: Media, requestBuilder: RequestBuilder<Drawable> ->
        requestBuilder
            .signature(MediaStoreSignature(media.mimeType, media.timestamp, media.orientation))
            .load(media.uri)
    }
    /** ************ **/

    Box(
        modifier = Modifier
            .semantics { isContainer = true }
            .zIndex(1f)
            .alpha(alpha)
            .fillMaxWidth()
    ) {
        /**
         * TODO: fillMaxWidth with fixed lateral padding on the search container only
         * It is not yet possible because of the material3 compose limitations
         */
        SearchBar(
            modifier = Modifier.align(Alignment.TopCenter),
            enabled = selectionState == null || !selectionState.value,
            query = query,
            onQueryChange = { query = it },
            onSearch = {
                if (it.isNotEmpty())
                    historySet = historySet.toMutableSet().apply { add(it) }
            },
            active = activeState,
            onActiveChange = { activeState = it },
            placeholder = {
                Text(text = stringResource(id = R.string.searchbar_title))
            },
            leadingIcon = {
                IconButton(onClick = {
                    activeState = !activeState
                    if (query.isNotEmpty()) query = ""
                }) {
                    val leadingIcon = if (activeState)
                        Icons.Outlined.ArrowBack else Icons.Outlined.Search
                    Icon(
                        imageVector = leadingIcon,
                        modifier = Modifier.fillMaxHeight(),
                        contentDescription = null
                    )
                }
            },
            trailingIcon = {
                Row {
                    if (!activeState) menuItems?.invoke(this)
                }
            },
            colors = SearchBarDefaults.colors(
                dividerColor = Color.Transparent
            )
        ) {
            /**
             * Recent searches
             */
            AnimatedVisibility(
                visible = query.isEmpty(),
                enter = enterAnimation,
                exit = exitAnimation
            ) {
                SearchHistory {
                    query = it
                }
            }

            /**
             * Searched content
             */
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = enterAnimation,
                exit = exitAnimation
            ) {
                if (state.media.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 72.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ImageSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(64.dp)
                        )
                        Text(
                            text = stringResource(R.string.no_media_found),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Adaptive(Dimens.Photo()),
                        contentPadding = PaddingValues(
                            bottom = bottomPadding + 16.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        items(
                            items = state.mappedMedia,
                            contentType = { it.key.startsWith("media_") },
                            span = { item ->
                                GridItemSpan(if (item.key.isHeaderKey) maxLineSpan else 1)
                            }
                        ) { item ->
                            when (item) {
                                is MediaItem.Header -> {
                                    val isChecked = rememberSaveable { mutableStateOf(false) }
                                    val title = item.text
                                        .replace("Today", stringToday)
                                        .replace("Yesterday", stringYesterday)
                                    StickyHeader(
                                        date = title,
                                        showAsBig = item.key.contains("big"),
                                        isCheckVisible = isChecked,
                                        isChecked = isChecked
                                    )
                                }

                                is MediaItem.MediaViewItem -> {
                                    val (media, preloadRequestBuilder) = preloadingData[state.media.indexOf(
                                        item.media
                                    )]
                                    MediaComponent(
                                        media = media,
                                        selectionState = vm.selectionState,
                                        selectedMedia = vm.selectedMedia,
                                        preloadRequestBuilder = preloadRequestBuilder,
                                        onItemLongClick = {},
                                        onItemClick = {
                                            dismissSearchBar()
                                            navigate(Screen.MediaViewScreen.route + "?mediaId=${it.id}")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    BackHandler(query.isNotEmpty()) {
        dismissSearchBar()
    }
}

@Composable
private fun SearchHistory(search: (query: String) -> Unit) {
    var historySet by rememberSearchHistory()
    val suggestionSet = listOf(
        "Screenshots",
        "Camera",
        "May 2022",
        "Thursday"
    )

    LazyColumn {
        if (historySet.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.history_recent_title),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(top = 16.dp)
                )
            }
        }
        items(historySet.toList()) {
            HistoryItem(
                historyQuery = it,
                search = search,
            ) {
                historySet = historySet.toMutableSet().apply { remove(it) }
            }
        }
        item {
            Text(
                text = stringResource(R.string.history_suggestions_title),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(top = 16.dp)
            )
        }
        items(suggestionSet) {
            HistoryItem(
                historyQuery = it,
                search = search,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.HistoryItem(
    historyQuery: String,
    search: (String) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = historyQuery,
                    modifier = Modifier
                        .weight(1f)
                )
                if (onDelete != null) {
                    IconButton(
                        onClick = {
                            onDelete(historyQuery)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null
                        )
                    }
                }
            }
        },
        leadingContent = {
            val icon = if (onDelete != null)
                Icons.Outlined.History
            else Icons.Outlined.Search
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .animateItemPlacement()
            .clickable { search(historyQuery) }
    )
}