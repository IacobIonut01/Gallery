/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.ignored.ui.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.core.ui.util.AppBottomSheetState
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.AlbumState
import com.dot.gallery.domain.model.IgnoredAlbum
import com.dot.gallery.domain.uri.emptyUri
import com.dot.gallery.feature.ignored.ui.IgnoredPreviewHost
import com.dot.gallery.feature.ignored.ui.setup.components.EditAlbumsStep
import com.dot.gallery.feature.ignored.ui.setup.components.EditConfirmationStep
import com.dot.gallery.feature.ignored.ui.setup.components.EditLocationStep
import com.dot.gallery.feature.ignored.ui.setup.components.OptionsMenuStep
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview


/**
 * Stateless shell for the options/edit sheet of an existing [IgnoredAlbum].
 * The app feeds [uiState] from `IgnoredOptionsViewModel` and forwards [onAction];
 * [onDismiss] runs whenever the sheet is closing so the caller can reset the VM.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun IgnoredOptionsSheet(
    sheetState: AppBottomSheetState,
    uiState: IgnoredOptionsUiState,
    albumState: State<AlbumState>,
    onAction: (IgnoredOptionsAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    BackHandler(sheetState.isVisible && uiState.currentStep != OptionsStep.OPTIONS) {
        onAction(IgnoredOptionsAction.NavigateBack)
    }

    if (sheetState.isVisible && uiState.ignoredAlbum != null) {
        val density = LocalDensity.current
        val dragHandleAlpha by remember {
            derivedStateOf {
                val offset = runCatching { sheetState.sheetState.requireOffset() }.getOrElse { Float.MAX_VALUE }
                val fadeThreshold = with(density) { 200.dp.toPx() }
                (offset / fadeThreshold).coerceIn(0f, 1f)
            }
        }

        ModalBottomSheet(
            sheetState = sheetState.sheetState,
            onDismissRequest = {
                scope.launch {
                    onDismiss()
                    sheetState.hide()
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 0.dp,
            dragHandle = { DragHandle(alpha = dragHandleAlpha) },
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            IgnoredOptionsSheetContent(
                uiState = uiState,
                albumState = albumState,
                onAction = { action ->
                    when (action) {
                        is IgnoredOptionsAction.Cancel -> {
                            scope.launch {
                                onDismiss()
                                sheetState.hide()
                            }
                        }
                        is IgnoredOptionsAction.Confirm -> {
                            onAction(action)
                            scope.launch {
                                onDismiss()
                                sheetState.hide()
                            }
                        }
                        is IgnoredOptionsAction.Delete -> {
                            onAction(action)
                            scope.launch {
                                onDismiss()
                                sheetState.hide()
                            }
                        }
                        else -> onAction(action)
                    }
                }
            )
        }
    }
}

@Composable
fun IgnoredOptionsSheetContent(
    uiState: IgnoredOptionsUiState,
    albumState: State<AlbumState>,
    onAction: (IgnoredOptionsAction) -> Unit
) {
    AnimatedContent(
        targetState = uiState.currentStep,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
            } else {
                slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
            }
        },
        label = "options_step_transition"
    ) { step ->
        when (step) {
            OptionsStep.OPTIONS -> {
                OptionsMenuStep(
                    ignoredAlbum = uiState.ignoredAlbum ?: return@AnimatedContent,
                    albumState = albumState,
                    isWildcard = uiState.isWildcard,
                    isMultiple = uiState.isMultiple,
                    onEdit = { onAction(IgnoredOptionsAction.Edit) },
                    onDelete = { onAction(IgnoredOptionsAction.Delete) },
                    onCancel = { onAction(IgnoredOptionsAction.Cancel) }
                )
            }

            OptionsStep.EDIT_LOCATION -> {
                EditLocationStep(
                    location = uiState.editLocation,
                    onLocationChanged = { onAction(IgnoredOptionsAction.SetLocation(it)) },
                    onBack = { onAction(IgnoredOptionsAction.NavigateBack) },
                    onNext = { onAction(IgnoredOptionsAction.NavigateNext) }
                )
            }

            OptionsStep.EDIT_ALBUMS -> {
                EditAlbumsStep(
                    isWildcard = uiState.isWildcard,
                    isMultiple = uiState.isMultiple,
                    regex = uiState.editRegex,
                    selectedAlbums = uiState.editSelectedAlbums,
                    albumState = albumState,
                    albumGridSize = uiState.albumGridSize,
                    onRegexChanged = { onAction(IgnoredOptionsAction.SetRegex(it)) },
                    onAlbumToggled = { album ->
                        if (uiState.isMultiple) {
                            onAction(IgnoredOptionsAction.ToggleAlbum(album))
                        } else {
                            onAction(IgnoredOptionsAction.SelectAlbum(album))
                        }
                    },
                    onBack = { onAction(IgnoredOptionsAction.NavigateBack) },
                    onNext = { onAction(IgnoredOptionsAction.NavigateNext) }
                )
            }

            OptionsStep.EDIT_CONFIRMATION -> {
                EditConfirmationStep(
                    location = uiState.editLocation,
                    isWildcard = uiState.isWildcard,
                    isMultiple = uiState.isMultiple,
                    regex = uiState.editRegex,
                    matchedAlbums = uiState.editMatchedAlbums,
                    albumGridSize = uiState.albumGridSize,
                    onBack = { onAction(IgnoredOptionsAction.NavigateBack) },
                    onConfirm = { onAction(IgnoredOptionsAction.Confirm) }
                )
            }
        }
    }
}

// ========== Previews ==========

private val mockAlbums = listOf(
    Album(id = 1, label = "Camera", uri = emptyUri, pathToThumbnail = "", relativePath = "DCIM/Camera", timestamp = 0, count = 42),
    Album(id = 2, label = "Screenshots", uri = emptyUri, pathToThumbnail = "", relativePath = "Pictures/Screenshots", timestamp = 0, count = 156),
    Album(id = 3, label = "Downloads", uri = emptyUri, pathToThumbnail = "", relativePath = "Download", timestamp = 0, count = 23)
)

@Preview
@Composable
private fun OptionsMenuSingleAlbumPreview() {
    IgnoredPreviewHost {
        val mockAlbumState = remember { mutableStateOf(AlbumState(albums = mockAlbums)) }
        IgnoredOptionsSheetContent(
            uiState = IgnoredOptionsUiState(
                currentStep = OptionsStep.OPTIONS,
                ignoredAlbum = IgnoredAlbum(
                    id = 1,
                    label = "Single #1",
                    location = IgnoredAlbum.ALBUMS_ONLY,
                    matchedAlbums = listOf("Camera")
                ),
                albums = mockAlbums
            ),
            albumState = mockAlbumState,
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun OptionsMenuMultipleAlbumsPreview() {
    IgnoredPreviewHost {
        val mockAlbumState = remember { mutableStateOf(AlbumState(albums = mockAlbums)) }
        IgnoredOptionsSheetContent(
            uiState = IgnoredOptionsUiState(
                currentStep = OptionsStep.OPTIONS,
                ignoredAlbum = IgnoredAlbum(
                    id = 1,
                    label = "Multiple #1",
                    albumIds = listOf(1L, 2L, 3L),
                    location = IgnoredAlbum.TIMELINE_ONLY,
                    matchedAlbums = listOf("Camera", "Screenshots", "Downloads")
                ),
                albums = mockAlbums
            ),
            albumState = mockAlbumState,
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun OptionsMenuRegexPreview() {
    IgnoredPreviewHost {
        val mockAlbumState = remember { mutableStateOf(AlbumState(albums = mockAlbums)) }
        IgnoredOptionsSheetContent(
            uiState = IgnoredOptionsUiState(
                currentStep = OptionsStep.OPTIONS,
                ignoredAlbum = IgnoredAlbum(
                    id = 1,
                    label = "Regex #1",
                    location = IgnoredAlbum.ALBUMS_AND_TIMELINE,
                    wildcard = "^Screenshot.*",
                    matchedAlbums = listOf("Screenshots", "Screenshots_old", "Screenshot_2024")
                ),
                albums = mockAlbums
            ),
            albumState = mockAlbumState,
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun EditLocationAlbumsOnlyPreview() {
    IgnoredPreviewHost {
        val mockAlbumState = remember { mutableStateOf(AlbumState(albums = mockAlbums)) }
        IgnoredOptionsSheetContent(
            uiState = IgnoredOptionsUiState(
                currentStep = OptionsStep.EDIT_LOCATION,
                ignoredAlbum = IgnoredAlbum(id = 1, label = "Single #1"),
                editLocation = IgnoredAlbum.ALBUMS_ONLY,
                albums = mockAlbums
            ),
            albumState = mockAlbumState,
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun EditLocationTimelineOnlyPreview() {
    IgnoredPreviewHost {
        val mockAlbumState = remember { mutableStateOf(AlbumState(albums = mockAlbums)) }
        IgnoredOptionsSheetContent(
            uiState = IgnoredOptionsUiState(
                currentStep = OptionsStep.EDIT_LOCATION,
                ignoredAlbum = IgnoredAlbum(id = 1, label = "Single #1"),
                editLocation = IgnoredAlbum.TIMELINE_ONLY,
                albums = mockAlbums
            ),
            albumState = mockAlbumState,
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun EditLocationBothPreview() {
    IgnoredPreviewHost {
        val mockAlbumState = remember { mutableStateOf(AlbumState(albums = mockAlbums)) }
        IgnoredOptionsSheetContent(
            uiState = IgnoredOptionsUiState(
                currentStep = OptionsStep.EDIT_LOCATION,
                ignoredAlbum = IgnoredAlbum(id = 1, label = "Single #1"),
                editLocation = IgnoredAlbum.ALBUMS_AND_TIMELINE,
                albums = mockAlbums
            ),
            albumState = mockAlbumState,
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun EditAlbumsRegexEmptyPreview() {
    IgnoredPreviewHost {
        val mockAlbumState = remember { mutableStateOf(AlbumState(albums = mockAlbums)) }
        IgnoredOptionsSheetContent(
            uiState = IgnoredOptionsUiState(
                currentStep = OptionsStep.EDIT_ALBUMS,
                ignoredAlbum = IgnoredAlbum(
                    id = 1,
                    label = "Regex #1",
                    wildcard = ""
                ),
                editRegex = "",
                albums = mockAlbums
            ),
            albumState = mockAlbumState,
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun EditAlbumsRegexWithPatternPreview() {
    IgnoredPreviewHost {
        val mockAlbumState = remember { mutableStateOf(AlbumState(albums = mockAlbums)) }
        IgnoredOptionsSheetContent(
            uiState = IgnoredOptionsUiState(
                currentStep = OptionsStep.EDIT_ALBUMS,
                ignoredAlbum = IgnoredAlbum(
                    id = 1,
                    label = "Regex #1",
                    wildcard = "^Screenshot.*"
                ),
                editRegex = "^Screenshot.*",
                albums = mockAlbums
            ),
            albumState = mockAlbumState,
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun EditAlbumsMultiplePreview() {
    IgnoredPreviewHost {
        val mockAlbumState = remember { mutableStateOf(AlbumState(albums = mockAlbums)) }
        IgnoredOptionsSheetContent(
            uiState = IgnoredOptionsUiState(
                currentStep = OptionsStep.EDIT_ALBUMS,
                ignoredAlbum = IgnoredAlbum(
                    id = 1,
                    label = "Multiple #1",
                    albumIds = listOf(1L, 2L)
                ),
                editSelectedAlbums = mockAlbums.take(2),
                albums = mockAlbums
            ),
            albumState = mockAlbumState,
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun EditConfirmationSinglePreview() {
    IgnoredPreviewHost {
        val mockAlbumState = remember { mutableStateOf(AlbumState(albums = mockAlbums)) }
        IgnoredOptionsSheetContent(
            uiState = IgnoredOptionsUiState(
                currentStep = OptionsStep.EDIT_CONFIRMATION,
                ignoredAlbum = IgnoredAlbum(id = 1, label = "Single #1"),
                editLocation = IgnoredAlbum.ALBUMS_ONLY,
                editMatchedAlbums = listOf(mockAlbums[0]),
                albums = mockAlbums
            ),
            albumState = mockAlbumState,
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun EditConfirmationMultiplePreview() {
    IgnoredPreviewHost {
        val mockAlbumState = remember { mutableStateOf(AlbumState(albums = mockAlbums)) }
        IgnoredOptionsSheetContent(
            uiState = IgnoredOptionsUiState(
                currentStep = OptionsStep.EDIT_CONFIRMATION,
                ignoredAlbum = IgnoredAlbum(
                    id = 1,
                    label = "Multiple #1",
                    albumIds = listOf(1L, 2L, 3L)
                ),
                editLocation = IgnoredAlbum.TIMELINE_ONLY,
                editMatchedAlbums = mockAlbums,
                albums = mockAlbums
            ),
            albumState = mockAlbumState,
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun EditConfirmationRegexPreview() {
    IgnoredPreviewHost {
        val mockAlbumState = remember { mutableStateOf(AlbumState(albums = mockAlbums)) }
        val matchedAlbums = listOf(
            Album(id = 1, label = "Screenshots", uri = emptyUri, pathToThumbnail = "", relativePath = "Pictures/Screenshots", timestamp = 0, count = 100),
            Album(id = 2, label = "Screenshot_2024", uri = emptyUri, pathToThumbnail = "", relativePath = "Pictures/Screenshot_2024", timestamp = 0, count = 50),
            Album(id = 3, label = "Screenshots_old", uri = emptyUri, pathToThumbnail = "", relativePath = "Pictures/Screenshots_old", timestamp = 0, count = 30)
        )
        IgnoredOptionsSheetContent(
            uiState = IgnoredOptionsUiState(
                currentStep = OptionsStep.EDIT_CONFIRMATION,
                ignoredAlbum = IgnoredAlbum(
                    id = 1,
                    label = "Regex #1",
                    wildcard = "^Screenshot.*"
                ),
                editLocation = IgnoredAlbum.ALBUMS_AND_TIMELINE,
                editRegex = "^Screenshot.*",
                editMatchedAlbums = matchedAlbums,
                albums = mockAlbums
            ),
            albumState = mockAlbumState,
            onAction = {}
        )
    }
}
