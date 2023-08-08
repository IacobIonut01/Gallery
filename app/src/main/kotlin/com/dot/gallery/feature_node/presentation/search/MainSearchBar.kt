/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Settings.Search.rememberSearchHistory
import com.dot.gallery.core.presentation.components.LoadingMedia
import com.dot.gallery.feature_node.presentation.common.components.MediaGridView
import com.dot.gallery.feature_node.presentation.search.SearchBarElevation.Collapsed
import com.dot.gallery.feature_node.presentation.search.SearchBarElevation.Expanded
import com.dot.gallery.feature_node.presentation.util.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSearchBar(
    bottomPadding: Dp,
    selectionState: MutableState<Boolean>? = null,
    navigate: (String) -> Unit,
    toggleNavbar: (Boolean) -> Unit,
    isScrolling: MutableState<Boolean>,
    menuItems: @Composable (RowScope.() -> Unit)? = null,
) {
    var historySet by rememberSearchHistory()
    val vm = hiltViewModel<SearchViewModel>()
    var query by rememberSaveable { mutableStateOf("") }
    val state by vm.mediaState.collectAsStateWithLifecycle()
    var activeState by rememberSaveable {
        mutableStateOf(false)
    }
    val dismissSearchBar = remember {
        {
            activeState = false
            query = ""
            vm.queryMedia(query)
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (selectionState != null && selectionState.value) 0.6f else 1f,
        label = "alpha"
    )
    val elevation by animateDpAsState(
        targetValue = if (activeState) Expanded() else Collapsed(),
        label = "elevation"
    )
    LaunchedEffect(LocalConfiguration.current, activeState) {
        if (selectionState == null || !selectionState.value)
            toggleNavbar(!activeState)
    }

    Box(
        modifier = Modifier
            .semantics { isTraversalGroup = true }
            .zIndex(1f)
            .alpha(alpha)
            .fillMaxWidth()
    ) {
        /**
         * TODO: fillMaxWidth with fixed lateral padding on the search container only
         * It is not yet possible because of the material3 compose limitations
         */
        val searchBarAlpha by animateFloatAsState(
            targetValue = if (isScrolling.value) 0f else 1f,
            label = "searchBarAlpha"
        )
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .alpha(searchBarAlpha),
            enabled = (selectionState == null || !selectionState.value) && !isScrolling.value,
            query = query,
            onQueryChange = {
                query = it
                if (it != vm.lastQuery.value && vm.lastQuery.value.isNotEmpty())
                    vm.clearQuery()
            },
            onSearch = {
                if (it.isNotEmpty())
                    historySet = historySet.toMutableSet().apply { add("${System.currentTimeMillis()}/$it") }
                vm.queryMedia(it)
            },
            active = activeState,
            onActiveChange = { activeState = it },
            placeholder = {
                Text(text = stringResource(id = R.string.searchbar_title))
            },
            tonalElevation = elevation,
            leadingIcon = {
                IconButton(onClick = {
                    activeState = !activeState
                    if (query.isNotEmpty()) query = ""
                    vm.queryMedia(query)
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
                visible = vm.lastQuery.value.isEmpty(),
                enter = enterAnimation,
                exit = exitAnimation
            ) {
                SearchHistory {
                    query = it
                    vm.queryMedia(it)
                }
            }

            /**
             * Searched content
             */
            AnimatedVisibility(
                visible = vm.lastQuery.value.isNotEmpty(),
                enter = enterAnimation,
                exit = exitAnimation
            ) {
                if (state.media.isEmpty() && !state.isLoading) {
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
                    val pd = PaddingValues(
                        bottom = bottomPadding + 16.dp
                    )
                    Box {
                        MediaGridView(
                            mediaState = state,
                            paddingValues = pd,
                            isScrolling = remember { mutableStateOf(false) }
                        ) {
                            dismissSearchBar()
                            navigate(Screen.MediaViewScreen.route + "?mediaId=${it.id}")
                        }
                        androidx.compose.animation.AnimatedVisibility(
                            visible = state.isLoading,
                            enter = enterAnimation,
                            exit = exitAnimation
                        ) {
                            LoadingMedia(paddingValues = pd)
                        }
                    }

                }
            }

        }
    }

    BackHandler(activeState) {
        if (vm.lastQuery.value.isEmpty())
            dismissSearchBar()
        else {
            query = ""
            vm.clearQuery()
        }
    }
}

@Composable
private fun SearchHistory(search: (query: String) -> Unit) {
    var historySet by rememberSearchHistory()
    val historyItems = remember(historySet) {
        historySet.toList().mapIndexed { index, item ->
            Pair(
                item.substringBefore(
                    delimiter = "/",
                    missingDelimiterValue = index.toString()
                ),
                item.substringAfter(
                    delimiter = "/",
                    missingDelimiterValue = item
                )
            )
        }.sortedByDescending { it.first }
    }
    val suggestionSet = listOf(
        "0" to "Screenshots",
        "1" to "Camera",
        "2" to "May 2022",
        "3" to "Thursday"
    )
    val maxItems = remember(historySet) {
        if (historyItems.size >= 5) 5 else historyItems.size
    }

    LazyColumn {
        if (historyItems.isNotEmpty()) {
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
            items(historyItems.subList(0, maxItems)) {
                HistoryItem(
                    historyQuery = it,
                    search = search,
                ) {
                    historySet = historySet.toMutableSet().apply { remove(it) }
                }
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
    historyQuery: Pair<String, String>,
    search: (String) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = historyQuery.second,
                    modifier = Modifier
                        .weight(1f)
                )
                if (onDelete != null) {
                    IconButton(
                        onClick = {
                            val timestamp = if (historyQuery.first.length < 10) "" else "${historyQuery.first}/"
                            onDelete("$timestamp${historyQuery.second}")
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
            .clickable { search(historyQuery.second) }
    )
}

sealed class SearchBarElevation(val dp: Dp) {

    object Collapsed : SearchBarElevation(2.dp)

    object Expanded : SearchBarElevation(0.dp)

    operator fun invoke() = dp
}