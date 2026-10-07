/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.ignored.ui.setup.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.domain.model.IgnoredAlbum
import com.dot.gallery.feature.ignored.ui.IgnoredPreviewHost
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.dot.gallery.feature.ignored.generated.resources.Res
import com.dot.gallery.feature.ignored.generated.resources.action_continue
import com.dot.gallery.feature.ignored.generated.resources.edit_location
import com.dot.gallery.feature.ignored.generated.resources.setup_location_location_subtitle
import com.dot.gallery.feature.ignored.generated.resources.setup_location_options_albums
import com.dot.gallery.feature.ignored.generated.resources.setup_location_options_both
import com.dot.gallery.feature.ignored.generated.resources.setup_location_options_timeline


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLocationStep(
    location: Int,
    onLocationChanged: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val albumsLabel = stringResource(Res.string.setup_location_options_albums)
    val timelineLabel = stringResource(Res.string.setup_location_options_timeline)
    val bothLabel = stringResource(Res.string.setup_location_options_both)

    val locationOptions = remember(albumsLabel, timelineLabel, bothLabel) {
        mapOf(
            albumsLabel to IgnoredAlbum.ALBUMS_ONLY,
            timelineLabel to IgnoredAlbum.TIMELINE_ONLY,
            bothLabel to IgnoredAlbum.ALBUMS_AND_TIMELINE,
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.edit_location),
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
                text = stringResource(Res.string.action_continue)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(Res.string.setup_location_location_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    locationOptions.entries.forEachIndexed { index, (label, value) ->
                        SegmentedButton(
                            selected = location == value,
                            onClick = { onLocationChanged(value) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = locationOptions.size
                            )
                        ) {
                            Text(
                                text = label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun EditLocationStepAlbumsOnlyPreview() {
    IgnoredPreviewHost {
        var location by remember { mutableIntStateOf(IgnoredAlbum.ALBUMS_ONLY) }
        EditLocationStep(
            location = location,
            onLocationChanged = { location = it },
            onBack = {},
            onNext = {}
        )
    }
}

@Preview
@Composable
private fun EditLocationStepTimelineOnlyPreview() {
    IgnoredPreviewHost {
        var location by remember { mutableIntStateOf(IgnoredAlbum.TIMELINE_ONLY) }
        EditLocationStep(
            location = location,
            onLocationChanged = { location = it },
            onBack = {},
            onNext = {}
        )
    }
}

@Preview
@Composable
private fun EditLocationStepBothPreview() {
    IgnoredPreviewHost {
        var location by remember { mutableIntStateOf(IgnoredAlbum.ALBUMS_AND_TIMELINE) }
        EditLocationStep(
            location = location,
            onLocationChanged = { location = it },
            onBack = {},
            onNext = {}
        )
    }
}
