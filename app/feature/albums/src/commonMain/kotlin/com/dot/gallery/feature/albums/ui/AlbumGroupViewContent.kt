/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.albums.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.LocalMediaDistributor
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.core.presentation.util.mediaSharedElement
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.AlbumGroupWithAlbums
import com.dot.gallery.feature.albums.ui.components.AlbumComponent
import com.dot.gallery.feature.albums.ui.components.ProviderIconSlot
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.albums.generated.resources.Res
import com.dot.gallery.feature.albums.generated.resources.no_groups
import com.dot.gallery.feature.albums.generated.resources.rename_group

/**
 * Grid of the albums inside a group. The app keeps the
 * MediaStore delete/trash launcher seam via [onMoveAlbumToTrash].
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumGroupViewContent(
    groupId: Long,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
    onMoveAlbumToTrash: (Album) -> Unit,
    onIgnoreAlbum: (Album) -> Unit,
    onRemoveFromGroup: (Album) -> Unit,
    onRenameGroup: (AlbumGroupWithAlbums) -> Unit,
    supportsTrash: Boolean = true,
    thumbnailActions: AlbumThumbnailActions? = null,
    providerIcon: ProviderIconSlot? = null,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    val distributor = LocalMediaDistributor.current
    val albumsState by distributor.albumsFlow.collectAsState()

    val group = albumsState.albumGroups.find { it.group.id == groupId }
    val groupAlbums = group?.albums ?: emptyList()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = group?.group?.label ?: "",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    NavigationBackButton()
                },
                actions = {
                    if (group != null) {
                        IconButton(onClick = { onRenameGroup(group) }) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = stringResource(Res.string.rename_group)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPaddingValues ->
        with(sharedTransitionScope) {
            LazyVerticalGrid(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    top = innerPaddingValues.calculateTopPadding(),
                    bottom = innerPaddingValues.calculateBottomPadding() + 16.dp + 64.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = groupAlbums,
                    key = { item -> item.toString() }
                ) { item ->
                    AlbumComponent(
                        modifier = Modifier.animateItem(),
                        thumbnailModifier = Modifier
                            .mediaSharedElement(
                                album = item,
                                animatedVisibilityScope = animatedContentScope
                            ),
                        album = item,
                        onItemClick = onAlbumClick,
                        onTogglePinClick = onAlbumLongClick,
                        onMoveAlbumToTrash = onMoveAlbumToTrash,
                        onToggleIgnoreClick = onIgnoreAlbum,
                        onRemoveFromGroup = onRemoveFromGroup,
                        supportsTrash = supportsTrash,
                        thumbnailActions = thumbnailActions,
                        providerIcon = providerIcon,
                    )
                }

                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "emptyGroup"
                ) {
                    AnimatedVisibility(
                        visible = groupAlbums.isEmpty(),
                        enter = enterAnimation,
                        exit = exitAnimation
                    ) {
                        Text(
                            text = stringResource(Res.string.no_groups),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
