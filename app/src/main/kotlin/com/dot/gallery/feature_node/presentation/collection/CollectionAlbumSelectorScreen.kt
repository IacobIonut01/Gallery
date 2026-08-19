/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import com.dot.gallery.core.presentation.components.SetupButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.LocalMediaDistributor
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.feature_node.presentation.settings.components.SettingsItem
import com.dot.gallery.feature_node.presentation.settings.components.settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Create mode: pass collectionName, onCreateWithAlbums, onSkip.
 * Edit mode:   pass collectionId, onReplaceAlbumsInCollection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionAlbumSelectorScreen(
    collectionName: String = "",
    collectionId: Long = -1L,
    onCreateWithAlbums: ((String, List<Long>) -> Unit)? = null,
    onSkip: ((String) -> Unit)? = null,
    onReplaceAlbumsInCollection: ((Long, List<Long>) -> Unit)? = null,
) {
    val isEditMode = collectionId > 0L
    val distributor = LocalMediaDistributor.current
    val albumsState by distributor.albumsFlow.collectAsStateWithLifecycle()
    val selectedAlbumIds = remember(collectionId) { mutableStateListOf<Long>() }
    var selectionInitialized by remember(collectionId) { mutableStateOf(!isEditMode) }

    if (isEditMode) {
        val collectionAlbumIdsFlow = remember(distributor, collectionId) {
            val flow: Flow<List<Long>?> = distributor.collectionAlbumIdsInCollection(collectionId)
                .map { albumIds -> albumIds.filter(::isSelectableCollectionAlbumId) }
            flow
        }
        val collectionAlbumIds by collectionAlbumIdsFlow
            .collectAsStateWithLifecycle(initialValue = null)
        LaunchedEffect(collectionAlbumIds) {
            if (!selectionInitialized && collectionAlbumIds != null) {
                selectedAlbumIds.addAll(collectionAlbumIds.orEmpty())
                selectionInitialized = true
            }
        }
    }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val titleRes = if (isEditMode) R.string.select_albums_for_collection
        else R.string.select_albums_for_collection

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = { NavigationBackButton() },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isEditMode) {
                    SetupButton(
                        onClick = {
                            onReplaceAlbumsInCollection?.invoke(collectionId, selectedAlbumIds.toList())
                        },
                        enabled = selectionInitialized,
                        applyHorizontalPadding = false,
                        applyBottomPadding = false,
                        applyInsets = false,
                        text = stringResource(R.string.save)
                    )
                } else {
                    SetupButton(
                        onClick = { onSkip?.invoke(collectionName) },
                        modifier = Modifier.weight(1f),
                        applyHorizontalPadding = false,
                        applyBottomPadding = false,
                        applyInsets = false,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        text = stringResource(R.string.skip)
                    )
                    SetupButton(
                        onClick = {
                            onCreateWithAlbums?.invoke(collectionName, selectedAlbumIds.toList())
                        },
                        enabled = selectedAlbumIds.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        applyHorizontalPadding = false,
                        applyBottomPadding = false,
                        applyInsets = false,
                        text = stringResource(R.string.add_selected)
                    )
                }
            }
        }
    ) { padding ->
        val subtitleText = stringResource(R.string.select_albums_for_collection_subtitle)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(LocalLayoutDirection.current),
                end = padding.calculateEndPadding(LocalLayoutDirection.current),
                top = 16.dp + padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp
            )
        ) {
            settings(
                preferenceItemBuilder = { item, modifier ->
                    SettingsItem(
                        item = item,
                        modifier = modifier,
                        tintIcon = false,
                        customTrailingContent = {
                            val tag = item.tag
                            if (tag is Long) {
                                Checkbox(
                                    checked = tag in selectedAlbumIds,
                                    onCheckedChange = null
                                )
                            }
                        }
                    )
                }
            ) {
                Header(subtitleText)
                for (album in albumsState.albums.filter { isSelectableCollectionAlbumId(it.id) }) {
                    Preference(
                        title = album.label,
                        icon = album.uri.toString(),
                        summary = "${album.count} items",
                        tag = album.id,
                        onClick = {
                            if (album.id in selectedAlbumIds) {
                                selectedAlbumIds.remove(album.id)
                            } else {
                                selectedAlbumIds.add(album.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

internal fun isSelectableCollectionAlbumId(albumId: Long): Boolean = albumId >= 0L
