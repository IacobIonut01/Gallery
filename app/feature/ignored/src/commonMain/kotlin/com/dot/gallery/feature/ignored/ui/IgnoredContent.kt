/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.ignored.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DisabledVisible
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.core.ui.components.AlbumPreferenceItem
import com.dot.gallery.core.ui.components.SettingsItem
import com.dot.gallery.core.ui.components.settings
import com.dot.gallery.domain.model.AlbumState
import com.dot.gallery.domain.model.IgnoredAlbum
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.dot.gallery.feature.ignored.generated.resources.Res
import com.dot.gallery.feature.ignored.generated.resources.ignored_albums
import com.dot.gallery.feature.ignored.generated.resources.ignored_albums_text
import com.dot.gallery.feature.ignored.generated.resources.matched_albums
import com.dot.gallery.feature.ignored.generated.resources.no_ignored_albums
import com.dot.gallery.feature.ignored.generated.resources.unknown_album
import com.dot.gallery.feature.ignored.generated.resources.wildcard_summary_first


/**
 * Substitutes `%1$s`/`%1$d`-style placeholders — needed where the settings DSL
 * builder (non-composable) consumes strings resolved via [stringResource].
 */
private fun String.formatArgs(vararg args: Any?): String {
    var result = this
    args.forEachIndexed { index, arg ->
        result = result
            .replace("%${index + 1}\$s", arg?.toString() ?: "")
            .replace("%${index + 1}\$d", arg?.toString() ?: "")
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IgnoredContent(
    state: IgnoredState,
    albumsState: State<AlbumState>,
    onAddClick: () -> Unit,
    onAlbumClick: (IgnoredAlbum) -> Unit
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val wildcardSummaryTemplate = stringResource(Res.string.wildcard_summary_first)
    val matchedSummaryTemplate = stringResource(Res.string.matched_albums)
    val unknownTitle = stringResource(Res.string.unknown_album)
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.ignored_albums),
                    )
                },
                navigationIcon = {
                    NavigationBackButton()
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.albums.isEmpty()) {
                    item {
                        NoIgnoredAlbums()
                    }
                    item {
                        Text(
                            modifier = Modifier
                                .padding(24.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(16.dp),
                            text = stringResource(Res.string.ignored_albums_text),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    item {
                        Spacer(Modifier.height(24.dp))
                    }

                    settings(
                        preferenceItemBuilder = { item, modifier ->
                            if (item is SettingsEntity.AlbumPreference) {
                                AlbumPreferenceItem(
                                    item = item,
                                    modifier = modifier
                                )
                            } else {
                                SettingsItem(
                                    item = item,
                                    modifier = modifier
                                )
                            }
                        }
                    ) {
                        state.albums.forEach { blacklistedAlbum ->
                            // Find matching albums in albumsState to get the thumbnail URIs
                            // First try to match by albumIds, then fallback to matching by the IgnoredAlbum.id
                            val albumIdsToMatch = blacklistedAlbum.albumIds.ifEmpty { listOf(blacklistedAlbum.id) }
                            val matchingAlbums = albumIdsToMatch.take(2).mapNotNull { albumId ->
                                albumsState.value.albumsWithBlacklisted.find { it.id == albumId }
                            }
                            val primaryAlbum = matchingAlbums.getOrNull(0)
                            val secondaryAlbum = matchingAlbums.getOrNull(1)
                            val isWildcard = blacklistedAlbum.wildcard != null
                            val isMultiple = blacklistedAlbum.albumIds.size > 1

                            AlbumPreference(
                                title = blacklistedAlbum.label
                                    ?: blacklistedAlbum.matchedAlbums.firstOrNull()
                                    ?: unknownTitle,
                                summary = if (isWildcard) {
                                    wildcardSummaryTemplate.formatArgs(
                                        blacklistedAlbum.wildcard ?: "",
                                        blacklistedAlbum.matchedAlbums.joinToString()
                                    )
                                } else matchedSummaryTemplate.formatArgs(
                                    blacklistedAlbum.matchedAlbums.joinToString()
                                ),
                                albumUri = primaryAlbum?.uri,
                                secondaryAlbumUri = secondaryAlbum?.uri,
                                albumLabel = primaryAlbum?.label,
                                albumCount = primaryAlbum?.count?.toInt() ?: 0,
                                matchedAlbumsCount = blacklistedAlbum.matchedAlbums.size,
                                isWildcard = isWildcard,
                                isMultiple = isMultiple,
                                onClick = {
                                    onAlbumClick(blacklistedAlbum)
                                }
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier
                    .padding(32.dp)
                    .align(Alignment.BottomEnd),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null
                )
            }

        }
    }
}

@Composable
fun NoIgnoredAlbums(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            modifier = Modifier.size(124.dp),
            imageVector = Icons.Outlined.DisabledVisible,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = null
        )

        Text(
            text = stringResource(Res.string.no_ignored_albums),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun IgnoredContentEmptyPreview() {
    IgnoredPreviewHost {
        IgnoredContent(
            state = IgnoredState(),
            albumsState = remember { mutableStateOf(AlbumState()) },
            onAddClick = {},
            onAlbumClick = {}
        )
    }
}

@Preview
@Composable
private fun IgnoredContentWithSingleAlbumPreview() {
    IgnoredPreviewHost {
        IgnoredContent(
            state = IgnoredState(
                albums = listOf(
                    IgnoredAlbum(
                        id = 1,
                        label = "Single #1",
                        location = IgnoredAlbum.ALBUMS_ONLY,
                        matchedAlbums = listOf("Camera")
                    )
                )
            ),
            albumsState = remember { mutableStateOf(AlbumState()) },
            onAddClick = {},
            onAlbumClick = {}
        )
    }
}

@Preview
@Composable
private fun IgnoredContentWithMultipleAlbumsPreview() {
    IgnoredPreviewHost {
        IgnoredContent(
            state = IgnoredState(
                albums = listOf(
                    IgnoredAlbum(
                        id = 100,
                        label = "Multiple #1",
                        albumIds = listOf(1, 2, 3),
                        location = IgnoredAlbum.ALBUMS_AND_TIMELINE,
                        matchedAlbums = listOf("Camera", "Screenshots", "Downloads")
                    )
                )
            ),
            albumsState = remember { mutableStateOf(AlbumState()) },
            onAddClick = {},
            onAlbumClick = {}
        )
    }
}

@Preview
@Composable
private fun IgnoredContentWithRegexPreview() {
    IgnoredPreviewHost {
        IgnoredContent(
            state = IgnoredState(
                albums = listOf(
                    IgnoredAlbum(
                        id = 200,
                        label = "Regex #1",
                        wildcard = ".*Music.*",
                        location = IgnoredAlbum.TIMELINE_ONLY,
                        matchedAlbums = listOf("Music", "Music Videos", "Background Music")
                    )
                )
            ),
            albumsState = remember { mutableStateOf(AlbumState()) },
            onAddClick = {},
            onAlbumClick = {}
        )
    }
}

@Preview
@Composable
private fun IgnoredContentMixedItemsPreview() {
    IgnoredPreviewHost {
        IgnoredContent(
            state = IgnoredState(
                albums = listOf(
                    IgnoredAlbum(
                        id = 1,
                        label = "Single #1",
                        location = IgnoredAlbum.ALBUMS_ONLY,
                        matchedAlbums = listOf("Camera")
                    ),
                    IgnoredAlbum(
                        id = 100,
                        label = "Multiple #1",
                        albumIds = listOf(1, 2, 3),
                        location = IgnoredAlbum.ALBUMS_AND_TIMELINE,
                        matchedAlbums = listOf("Screenshots", "Downloads", "Telegram")
                    ),
                    IgnoredAlbum(
                        id = 200,
                        label = "Regex #1",
                        wildcard = ".*backup.*",
                        location = IgnoredAlbum.TIMELINE_ONLY,
                        matchedAlbums = listOf("WhatsApp Backup", "Phone Backup")
                    )
                )
            ),
            albumsState = remember { mutableStateOf(AlbumState()) },
            onAddClick = {},
            onAlbumClick = {}
        )
    }
}

@Preview
@Composable
private fun NoIgnoredAlbumsPreview() {
    IgnoredPreviewHost {
        NoIgnoredAlbums()
    }
}
