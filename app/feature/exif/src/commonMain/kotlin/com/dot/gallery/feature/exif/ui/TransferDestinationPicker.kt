/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.exif.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Constants.albumCellsList
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.AlbumGroupWithAlbums
import com.dot.gallery.feature.albums.ui.components.AlbumComponent
import com.dot.gallery.feature.albums.ui.components.AlbumGroupComponent
import com.dot.gallery.feature.albums.ui.components.ProviderIconSlot
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.exif.generated.resources.Res
import com.dot.gallery.feature.exif.generated.resources.transfer_on_device
import com.dot.gallery.feature.exif.generated.resources.transfer_unavailable_cloud

/**
 * Shared album destination picker used by the move/copy sheets: group back
 * navigation, filtered groups, and per-account destination sections.
 */
@Composable
internal fun TransferDestinationPicker(
    albums: List<Album>,
    albumGroups: List<AlbumGroupWithAlbums>,
    cloudAccounts: Map<Long, CloudDestinationAccount>,
    query: String,
    onQueryChange: (String) -> Unit,
    gridSizeIndex: Int,
    newAlbumEnabled: Boolean,
    fallbackStatus: CloudCopyDestinationStatus?,
    isDestinationEnabled: (Album) -> Boolean,
    destinationStatus: (Album) -> CloudCopyDestinationStatus?,
    onAlbumClick: (Album) -> Unit,
    onNewAlbumClick: () -> Unit,
    providerIcon: ProviderIconSlot?,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val allGroups = albumGroups
    val groupedAlbumIds = remember(allGroups) {
        allGroups.flatMap { g -> g.albums.map { it.id } }.toSet()
    }
    val allUngroupedAlbums = remember(albums, groupedAlbumIds) {
        albums.filter { it.id !in groupedAlbumIds }
    }
    var selectedGroup by remember { mutableStateOf<AlbumGroupWithAlbums?>(null) }
    // Keep selectedGroup in sync with latest data
    val liveSelectedGroup = selectedGroup?.let { sel ->
        allGroups.find { it.group.id == sel.group.id }
    }

    val trimmedQuery = query.trim()
    val filteredGroups = remember(allGroups, trimmedQuery) {
        if (trimmedQuery.isEmpty()) allGroups
        else allGroups.mapNotNull { g ->
            val matched = g.albums.filter { it.label.contains(trimmedQuery, ignoreCase = true) }
            if (matched.isNotEmpty()) g.copy(albums = matched)
            else if (g.group.label.contains(trimmedQuery, ignoreCase = true)) g
            else null
        }
    }
    val filteredUngroupedAlbums = remember(allUngroupedAlbums, trimmedQuery) {
        if (trimmedQuery.isEmpty()) allUngroupedAlbums
        else allUngroupedAlbums.filter { it.label.contains(trimmedQuery, ignoreCase = true) }
    }
    val filteredGroupAlbums = remember(liveSelectedGroup, trimmedQuery) {
        val groupAlbums = liveSelectedGroup?.albums ?: emptyList()
        if (trimmedQuery.isEmpty()) groupAlbums
        else groupAlbums.filter { it.label.contains(trimmedQuery, ignoreCase = true) }
    }
    val localSectionTitle = stringResource(Res.string.transfer_on_device)
    val unavailableSectionTitle = stringResource(Res.string.transfer_unavailable_cloud)
    val destinationSections = remember(
        filteredUngroupedAlbums,
        cloudAccounts,
        localSectionTitle,
        unavailableSectionTitle
    ) {
        buildTransferDestinationSections(
            albums = filteredUngroupedAlbums,
            accounts = cloudAccounts,
            localTitle = localSectionTitle,
            unavailableTitle = unavailableSectionTitle
        )
    }

    LazyVerticalGrid(
        state = gridState,
        modifier = modifier.padding(horizontal = 8.dp),
        columns = albumCellsList[gridSizeIndex.coerceIn(0, albumCellsList.lastIndex)],
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(
            bottom = WindowInsets.navigationBars.getBottom(LocalDensity.current).dp
        )
    ) {
        if (liveSelectedGroup != null) {
            // Group detail view
            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "group_back_header"
            ) {
                PickerGroupBackHeader(
                    group = liveSelectedGroup,
                    onBack = {
                        selectedGroup = null
                        onQueryChange("")
                    }
                )
            }
            items(
                items = filteredGroupAlbums,
                key = { item -> "group_album_${item.id}" }
            ) { item ->
                DestinationAlbumCell(
                    album = item,
                    fallbackStatus = fallbackStatus,
                    isDestinationEnabled = isDestinationEnabled,
                    destinationStatus = destinationStatus,
                    onAlbumClick = onAlbumClick,
                    providerIcon = providerIcon,
                    modifier = Modifier.animateItem()
                )
            }
        } else {
            // Main view: New Album + groups + ungrouped albums
            if (trimmedQuery.isEmpty()) {
                item {
                    AlbumComponent(
                        album = Album.NewAlbum,
                        isEnabled = newAlbumEnabled,
                        onItemClick = { onNewAlbumClick() }
                    )
                }
            }

            items(
                items = filteredGroups,
                key = { group -> "group_${group.group.id}" }
            ) { group ->
                AlbumGroupComponent(
                    modifier = Modifier.animateItem(),
                    groupWithAlbums = group,
                    onGroupClick = { selectedGroup = it }
                )
            }

            destinationSections.forEach { section ->
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "destination_header_${section.key}"
                ) {
                    TransferDestinationSectionHeader(
                        section = section,
                        providerIcon = providerIcon
                    )
                }
                items(
                    items = section.albums,
                    key = { item -> "${section.key}_${item.id}" }
                ) { item ->
                    DestinationAlbumCell(
                        album = item,
                        fallbackStatus = fallbackStatus,
                        isDestinationEnabled = isDestinationEnabled,
                        destinationStatus = destinationStatus,
                        onAlbumClick = onAlbumClick,
                        providerIcon = providerIcon
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationAlbumCell(
    album: Album,
    fallbackStatus: CloudCopyDestinationStatus?,
    isDestinationEnabled: (Album) -> Boolean,
    destinationStatus: (Album) -> CloudCopyDestinationStatus?,
    onAlbumClick: (Album) -> Unit,
    providerIcon: ProviderIconSlot?,
    modifier: Modifier = Modifier,
) {
    val cloudStatus = destinationStatus(album)
    val isEnabled = isDestinationEnabled(album)
    val disabledDescription = when {
        fallbackStatus != null && fallbackStatus != CloudCopyDestinationStatus.READY ->
            stringResource(fallbackStatus.messageRes())
        cloudStatus != null && cloudStatus != CloudCopyDestinationStatus.READY ->
            stringResource(cloudStatus.messageRes())
        else -> null
    }
    AlbumComponent(
        modifier = modifier.semantics {
            if (!isEnabled && disabledDescription != null) {
                disabled()
                stateDescription = disabledDescription
            }
        },
        album = album,
        isEnabled = isEnabled,
        onItemClick = onAlbumClick,
        providerIcon = providerIcon
    )
}
