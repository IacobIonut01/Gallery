/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.ignored.ui.setup.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Constants.albumCellsList
import com.dot.gallery.core.presentation.components.LocalMediaImageRenderer
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.IgnoredAlbum
import com.dot.gallery.domain.uri.emptyUri
import com.dot.gallery.feature.ignored.ui.IgnoredPreviewHost
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock
import com.dot.gallery.feature.ignored.generated.resources.Res
import com.dot.gallery.feature.ignored.generated.resources.action_save
import com.dot.gallery.feature.ignored.generated.resources.setup_confirmation_albums
import com.dot.gallery.feature.ignored.generated.resources.setup_confirmation_location
import com.dot.gallery.feature.ignored.generated.resources.setup_confirmation_preview
import com.dot.gallery.feature.ignored.generated.resources.setup_confirmation_regex
import com.dot.gallery.feature.ignored.generated.resources.setup_confirmation_title
import com.dot.gallery.feature.ignored.generated.resources.setup_confirmation_type
import com.dot.gallery.feature.ignored.generated.resources.setup_location_options_albums
import com.dot.gallery.feature.ignored.generated.resources.setup_location_options_both
import com.dot.gallery.feature.ignored.generated.resources.setup_location_options_timeline
import com.dot.gallery.feature.ignored.generated.resources.setup_type_multiple
import com.dot.gallery.feature.ignored.generated.resources.setup_type_regex
import com.dot.gallery.feature.ignored.generated.resources.setup_type_single


@OptIn(ExperimentalMaterial3Api::class)
@Stable
@Composable
fun EditConfirmationStep(
    location: Int,
    isWildcard: Boolean,
    isMultiple: Boolean,
    regex: String,
    matchedAlbums: List<Album>,
    /** Index into [albumCellsList] — see `IgnoredOptionsUiState.albumGridSize`. */
    albumGridSize: Int,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    val gridCells = remember(albumGridSize) {
        albumCellsList[albumGridSize.coerceIn(4, albumCellsList.lastIndex)]
    }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.setup_confirmation_title),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    NavigationBackButton(
                        forcedAction = onBack
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            SetupButton(
                onClick = onConfirm,
                text = stringResource(Res.string.action_save)
            )
        }
    ) {
        LazyVerticalGrid(
            columns = gridCells,
            modifier = Modifier
                .padding(it)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item(span = { GridItemSpan(maxLineSpan) }) {
                ConfirmationSummaryCard(
                    location = location,
                    isWildcard = isWildcard,
                    isMultiple = isMultiple,
                    regex = regex,
                    albumCount = matchedAlbums.size
                )
            }

            if (matchedAlbums.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        ),
                        text = stringResource(Res.string.setup_confirmation_preview),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(
                    items = matchedAlbums,
                    key = { it.id }
                ) { album ->
                    ConfirmationAlbumItem(album = album)
                }
            }
        }
    }
}

@Composable
fun ConfirmationSummaryCard(
    location: Int,
    isWildcard: Boolean,
    isMultiple: Boolean,
    regex: String,
    albumCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Type
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.setup_confirmation_type),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = when {
                    isWildcard -> stringResource(Res.string.setup_type_regex)
                    isMultiple -> stringResource(Res.string.setup_type_multiple)
                    else -> stringResource(Res.string.setup_type_single)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Location
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.setup_confirmation_location),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = when (location) {
                    IgnoredAlbum.ALBUMS_ONLY -> stringResource(Res.string.setup_location_options_albums)
                    IgnoredAlbum.TIMELINE_ONLY -> stringResource(Res.string.setup_location_options_timeline)
                    else -> stringResource(Res.string.setup_location_options_both)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Albums count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.setup_confirmation_albums),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$albumCount",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (isWildcard) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(Res.string.setup_confirmation_regex),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = regex,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ConfirmationAlbumItem(
    album: Album,
    modifier: Modifier = Modifier
) {
    Column {
        LocalMediaImageRenderer.current.RenderImage(
            model = album.uri,
            contentDescription = album.label,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .aspectRatio(1f),
            signature = album
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            text = album.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    )
)

@Preview
@Composable
private fun EditConfirmationStepSinglePreview() {
    IgnoredPreviewHost {
        EditConfirmationStep(
            location = IgnoredAlbum.ALBUMS_ONLY,
            isWildcard = false,
            isMultiple = false,
            regex = "",
            matchedAlbums = listOf(previewAlbums[0]),
            albumGridSize = 4,
            onBack = {},
            onConfirm = {}
        )
    }
}

@Preview
@Composable
private fun EditConfirmationStepMultiplePreview() {
    IgnoredPreviewHost {
        EditConfirmationStep(
            location = IgnoredAlbum.ALBUMS_AND_TIMELINE,
            isWildcard = false,
            isMultiple = true,
            regex = "",
            matchedAlbums = previewAlbums,
            albumGridSize = 4,
            onBack = {},
            onConfirm = {}
        )
    }
}

@Preview
@Composable
private fun EditConfirmationStepRegexPreview() {
    IgnoredPreviewHost {
        EditConfirmationStep(
            location = IgnoredAlbum.TIMELINE_ONLY,
            isWildcard = true,
            isMultiple = false,
            regex = ".*Music.*",
            matchedAlbums = previewAlbums,
            albumGridSize = 4,
            onBack = {},
            onConfirm = {}
        )
    }
}

@Preview
@Composable
private fun ConfirmationSummaryCardSinglePreview() {
    IgnoredPreviewHost {
        ConfirmationSummaryCard(
            location = IgnoredAlbum.ALBUMS_ONLY,
            isWildcard = false,
            isMultiple = false,
            regex = "",
            albumCount = 1,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview
@Composable
private fun ConfirmationSummaryCardRegexPreview() {
    IgnoredPreviewHost {
        ConfirmationSummaryCard(
            location = IgnoredAlbum.TIMELINE_ONLY,
            isWildcard = true,
            isMultiple = false,
            regex = ".*Downloads.*",
            albumCount = 5,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview
@Composable
private fun ConfirmationAlbumItemPreview() {
    IgnoredPreviewHost {
        ConfirmationAlbumItem(
            album = previewAlbums[0],
            modifier = Modifier.padding(8.dp)
        )
    }
}
