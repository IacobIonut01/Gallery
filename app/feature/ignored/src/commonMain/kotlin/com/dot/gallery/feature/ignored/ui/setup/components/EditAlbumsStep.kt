/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.ignored.ui.setup.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Constants.albumCellsList
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.AlbumState
import com.dot.gallery.domain.model.matchesAlbum
import com.dot.gallery.domain.uri.emptyUri
import com.dot.gallery.feature.ignored.ui.IgnoredPreviewHost
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock
import com.dot.gallery.feature.ignored.generated.resources.Res
import com.dot.gallery.feature.ignored.generated.resources.action_continue
import com.dot.gallery.feature.ignored.generated.resources.edit_albums
import com.dot.gallery.feature.ignored.generated.resources.setup_album_multiple_select
import com.dot.gallery.feature.ignored.generated.resources.setup_album_regex_matched
import com.dot.gallery.feature.ignored.generated.resources.setup_album_regex_placeholder
import com.dot.gallery.feature.ignored.generated.resources.setup_album_single_select


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlbumsStep(
    isWildcard: Boolean,
    isMultiple: Boolean,
    regex: String,
    selectedAlbums: List<Album>,
    albumState: State<AlbumState>,
    /**
     * Index into [albumCellsList] — resolved app-side from the album grid-size
     * preference and carried by `IgnoredOptionsUiState.albumGridSize`.
     */
    albumGridSize: Int,
    onRegexChanged: (String) -> Unit,
    onAlbumToggled: (Album) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val albums by remember(albumState.value.albumsWithBlacklisted) {
        derivedStateOf { albumState.value.albumsWithBlacklisted }
    }
    val gridState = rememberLazyGridState()
    val gridCells = remember(albumGridSize) {
        albumCellsList[albumGridSize.coerceIn(4, albumCellsList.lastIndex)]
    }

    val regexMatchedAlbums = remember(regex, albums) {
        if (isWildcard && regex.isNotEmpty()) {
            try {
                val regexPattern = regex.toRegex()
                albums.filter(regexPattern::matchesAlbum)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    val canProceed by remember(isWildcard, isMultiple, regex, regexMatchedAlbums, selectedAlbums) {
        derivedStateOf {
            when {
                isWildcard -> regex.isNotEmpty() && regexMatchedAlbums.isNotEmpty()
                isMultiple -> selectedAlbums.size >= 2
                else -> selectedAlbums.isNotEmpty()
            }
        }
    }

    val displayAlbums by remember(isWildcard, regexMatchedAlbums, albums) {
        derivedStateOf {
            if (isWildcard) regexMatchedAlbums else albums
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.edit_albums),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    NavigationBackButton(forcedAction = onBack)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            SetupButton(
                onClick = onNext,
                enabled = canProceed,
                text = stringResource(Res.string.action_continue)
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            state = gridState,
            columns = gridCells,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isWildcard) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = regex,
                            onValueChange = onRegexChanged,
                            label = { Text(stringResource(Res.string.setup_album_regex_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        if (regexMatchedAlbums.isNotEmpty()) {
                            Text(
                                text = stringResource(Res.string.setup_album_regex_matched, regexMatchedAlbums.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = if (isMultiple) {
                            stringResource(Res.string.setup_album_multiple_select)
                        } else {
                            stringResource(Res.string.setup_album_single_select)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            items(
                items = displayAlbums,
                key = { it.id }
            ) { album ->
                val isSelected = if (isWildcard) true else album in selectedAlbums
                EditAlbumItem(
                    album = album,
                    isSelected = isSelected,
                    onClick = { if (!isWildcard) onAlbumToggled(album) }
                )
            }
        }
    }
}

private val previewAlbums = listOf(
    Album(
        id = 1,
        label = "Camera",
        uri = emptyUri,
        pathToThumbnail = "",
        relativePath = "DCIM/Camera",
        timestamp = Clock.System.now().toEpochMilliseconds(),
        count = 100
    ),
    Album(
        id = 2,
        label = "Screenshots",
        uri = emptyUri,
        pathToThumbnail = "",
        relativePath = "Pictures/Screenshots",
        timestamp = Clock.System.now().toEpochMilliseconds(),
        count = 50
    ),
    Album(
        id = 3,
        label = "Downloads",
        uri = emptyUri,
        pathToThumbnail = "",
        relativePath = "Download",
        timestamp = Clock.System.now().toEpochMilliseconds(),
        count = 25
    )
)

@Preview
@Composable
private fun EditAlbumsStepSinglePreview() {
    IgnoredPreviewHost {
        val selectedAlbums = remember { mutableStateListOf<Album>() }
        EditAlbumsStep(
            isWildcard = false,
            isMultiple = false,
            regex = "",
            selectedAlbums = selectedAlbums,
            albumState = remember { mutableStateOf(AlbumState(albumsWithBlacklisted = previewAlbums)) },
            albumGridSize = 4,
            onRegexChanged = {},
            onAlbumToggled = { album ->
                selectedAlbums.clear()
                selectedAlbums.add(album)
            },
            onBack = {},
            onNext = {}
        )
    }
}

@Preview
@Composable
private fun EditAlbumsStepMultiplePreview() {
    IgnoredPreviewHost {
        val selectedAlbums = remember { mutableStateListOf(previewAlbums[0], previewAlbums[1]) }
        EditAlbumsStep(
            isWildcard = false,
            isMultiple = true,
            regex = "",
            selectedAlbums = selectedAlbums,
            albumState = remember { mutableStateOf(AlbumState(albumsWithBlacklisted = previewAlbums)) },
            albumGridSize = 4,
            onRegexChanged = {},
            onAlbumToggled = { album ->
                if (album in selectedAlbums) {
                    selectedAlbums.remove(album)
                } else {
                    selectedAlbums.add(album)
                }
            },
            onBack = {},
            onNext = {}
        )
    }
}

@Preview
@Composable
private fun EditAlbumsStepRegexEmptyPreview() {
    IgnoredPreviewHost {
        var regex by remember { mutableStateOf("") }
        EditAlbumsStep(
            isWildcard = true,
            isMultiple = false,
            regex = regex,
            selectedAlbums = emptyList(),
            albumState = remember { mutableStateOf(AlbumState(albumsWithBlacklisted = previewAlbums)) },
            albumGridSize = 4,
            onRegexChanged = { regex = it },
            onAlbumToggled = {},
            onBack = {},
            onNext = {}
        )
    }
}

@Preview
@Composable
private fun EditAlbumsStepRegexWithMatchesPreview() {
    IgnoredPreviewHost {
        var regex by remember { mutableStateOf(".*Camera.*") }
        EditAlbumsStep(
            isWildcard = true,
            isMultiple = false,
            regex = regex,
            selectedAlbums = emptyList(),
            albumState = remember { mutableStateOf(AlbumState(albumsWithBlacklisted = previewAlbums)) },
            albumGridSize = 4,
            onRegexChanged = { regex = it },
            onAlbumToggled = {},
            onBack = {},
            onNext = {}
        )
    }
}
