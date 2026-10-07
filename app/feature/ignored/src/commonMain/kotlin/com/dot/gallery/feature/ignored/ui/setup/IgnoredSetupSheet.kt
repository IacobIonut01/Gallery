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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Constants
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.core.ui.util.AppBottomSheetState
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.IgnoredAlbum
import com.dot.gallery.domain.model.matchesAlbum
import com.dot.gallery.domain.uri.emptyUri
import com.dot.gallery.feature.ignored.ui.IgnoredPreviewHost
import com.dot.gallery.feature.ignored.ui.setup.components.ConfirmationCard
import com.dot.gallery.feature.ignored.ui.setup.components.RegexExample
import com.dot.gallery.feature.ignored.ui.setup.components.SectionHeader
import com.dot.gallery.feature.ignored.ui.setup.components.SelectableAlbumItem
import com.dot.gallery.feature.ignored.ui.setup.components.TypeOptionCard
import com.dot.gallery.ui.core.Icons as GalleryIcons
import com.dot.gallery.ui.core.icons.RegularExpression
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.dot.gallery.feature.ignored.generated.resources.Res
import com.dot.gallery.feature.ignored.generated.resources.action_cancel
import com.dot.gallery.feature.ignored.generated.resources.albums
import com.dot.gallery.feature.ignored.generated.resources.albums_and_timeline
import com.dot.gallery.feature.ignored.generated.resources.albums_count
import com.dot.gallery.feature.ignored.generated.resources.apply
import com.dot.gallery.feature.ignored.generated.resources.continue_string
import com.dot.gallery.feature.ignored.generated.resources.continue_with_count
import com.dot.gallery.feature.ignored.generated.resources.go_back
import com.dot.gallery.feature.ignored.generated.resources.matched_more_count
import com.dot.gallery.feature.ignored.generated.resources.no_matches
import com.dot.gallery.feature.ignored.generated.resources.selected_count
import com.dot.gallery.feature.ignored.generated.resources.setup_album_regex_matched
import com.dot.gallery.feature.ignored.generated.resources.setup_confirmation_matched
import com.dot.gallery.feature.ignored.generated.resources.setup_confirmation_subtitle
import com.dot.gallery.feature.ignored.generated.resources.setup_confirmation_title
import com.dot.gallery.feature.ignored.generated.resources.setup_confirmation_where
import com.dot.gallery.feature.ignored.generated.resources.setup_confirmation_who
import com.dot.gallery.feature.ignored.generated.resources.setup_location_location_subtitle
import com.dot.gallery.feature.ignored.generated.resources.setup_location_location_title
import com.dot.gallery.feature.ignored.generated.resources.setup_location_options_albums
import com.dot.gallery.feature.ignored.generated.resources.setup_location_options_both
import com.dot.gallery.feature.ignored.generated.resources.setup_location_options_timeline
import com.dot.gallery.feature.ignored.generated.resources.setup_location_title
import com.dot.gallery.feature.ignored.generated.resources.setup_location_type_subtitle
import com.dot.gallery.feature.ignored.generated.resources.setup_location_type_title
import com.dot.gallery.feature.ignored.generated.resources.setup_location_types_multiple
import com.dot.gallery.feature.ignored.generated.resources.setup_location_types_regex
import com.dot.gallery.feature.ignored.generated.resources.setup_location_types_single
import com.dot.gallery.feature.ignored.generated.resources.setup_type_multiple_subtitle
import com.dot.gallery.feature.ignored.generated.resources.setup_type_multiple_title
import com.dot.gallery.feature.ignored.generated.resources.setup_type_regex_error
import com.dot.gallery.feature.ignored.generated.resources.setup_type_regex_error_second
import com.dot.gallery.feature.ignored.generated.resources.setup_type_regex_example_first_title
import com.dot.gallery.feature.ignored.generated.resources.setup_type_regex_example_second_subtitle
import com.dot.gallery.feature.ignored.generated.resources.setup_type_regex_example_second_title
import com.dot.gallery.feature.ignored.generated.resources.setup_type_regex_example_third_subtitle
import com.dot.gallery.feature.ignored.generated.resources.setup_type_regex_example_third_title
import com.dot.gallery.feature.ignored.generated.resources.setup_type_regex_example_title
import com.dot.gallery.feature.ignored.generated.resources.setup_type_regex_first_subtitle
import com.dot.gallery.feature.ignored.generated.resources.setup_type_regex_label
import com.dot.gallery.feature.ignored.generated.resources.setup_type_regex_subtitle
import com.dot.gallery.feature.ignored.generated.resources.setup_type_regex_summary
import com.dot.gallery.feature.ignored.generated.resources.setup_type_regex_title
import com.dot.gallery.feature.ignored.generated.resources.setup_type_selection_subtitle
import com.dot.gallery.feature.ignored.generated.resources.setup_type_selection_title
import com.dot.gallery.feature.ignored.generated.resources.timeline
import com.dot.gallery.feature.ignored.generated.resources.unknown_album


/**
 * Stateless sheet shell for the ignore-setup wizard. The app provides the
 * [uiState] (driven by `IgnoredSetupViewModel`) and funnels [onAction] back to
 * it; [onDismiss] is invoked whenever the sheet is closing so the caller can
 * reset the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun IgnoredSetupSheet(
    sheetState: AppBottomSheetState,
    uiState: IgnoredSetupUiState,
    onAction: (IgnoredSetupAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    BackHandler(sheetState.isVisible && uiState.currentStep != SetupStep.TYPE_SELECTION) {
        onAction(IgnoredSetupAction.NavigateBack)
    }

    if (sheetState.isVisible) {
        val density = LocalDensity.current
        val dragHandleAlpha by remember {
            derivedStateOf {
                val offset =
                    runCatching { sheetState.sheetState.requireOffset() }.getOrElse { Float.MAX_VALUE }
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
            IgnoredSetupSheetContent(
                uiState = uiState,
                onAction = { action ->
                    when (action) {
                        is IgnoredSetupAction.Cancel -> {
                            scope.launch {
                                onDismiss()
                                sheetState.hide()
                            }
                        }

                        is IgnoredSetupAction.Confirm -> {
                            onAction(action)
                            scope.launch {
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
fun IgnoredSetupSheetContent(
    uiState: IgnoredSetupUiState,
    onAction: (IgnoredSetupAction) -> Unit
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
        label = "step_transition"
    ) { step ->
        when (step) {
            SetupStep.TYPE_SELECTION -> {
                TypeSelectionStepContent(
                    uiState = uiState,
                    onAction = onAction
                )
            }

            SetupStep.ALBUM_SELECTION -> {
                AlbumSelectionStepContent(
                    uiState = uiState,
                    onAction = onAction
                )
            }

            SetupStep.CONFIRMATION -> {
                ConfirmationStepContent(
                    uiState = uiState,
                    onAction = onAction
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSelectionStepContent(
    uiState: IgnoredSetupUiState,
    onAction: (IgnoredSetupAction) -> Unit
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
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.setup_location_title),
                        textAlign = TextAlign.Center
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SetupButton(
                    onClick = { onAction(IgnoredSetupAction.Cancel) },
                    modifier = Modifier.weight(1f),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    text = stringResource(Res.string.action_cancel)
                )
                SetupButton(
                    onClick = { onAction(IgnoredSetupAction.NavigateNext) },
                    modifier = Modifier.weight(1f),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    text = stringResource(Res.string.continue_string)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SectionHeader(
                    title = stringResource(Res.string.setup_location_location_title),
                    subtitle = stringResource(Res.string.setup_location_location_subtitle),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    locationOptions.onEachIndexed { index, (option, optLocation) ->
                        SegmentedButton(
                            selected = uiState.location == optLocation,
                            onClick = { onAction(IgnoredSetupAction.SetLocation(optLocation)) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = locationOptions.size
                            )
                        ) {
                            Text(text = option, maxLines = 1)
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = stringResource(Res.string.setup_location_type_title),
                    subtitle = stringResource(Res.string.setup_location_type_subtitle),
                    modifier = Modifier
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypeOptionCard(
                        icon = Icons.Outlined.PhotoAlbum,
                        title = stringResource(Res.string.setup_location_types_single),
                        isSelected = uiState.type is IgnoredType.SINGLE,
                        onClick = {
                            onAction(
                                IgnoredSetupAction.SetType(
                                    IgnoredType.SINGLE(
                                        null
                                    )
                                )
                            )
                        }
                    )
                    TypeOptionCard(
                        icon = Icons.Outlined.FolderCopy,
                        title = stringResource(Res.string.setup_location_types_multiple),
                        isSelected = uiState.type is IgnoredType.MULTIPLE,
                        onClick = {
                            onAction(
                                IgnoredSetupAction.SetType(
                                    IgnoredType.MULTIPLE(
                                        emptyList()
                                    )
                                )
                            )
                        }
                    )
                    TypeOptionCard(
                        icon = GalleryIcons.RegularExpression,
                        title = stringResource(Res.string.setup_location_types_regex),
                        isSelected = uiState.type is IgnoredType.REGEX,
                        onClick = { onAction(IgnoredSetupAction.SetType(IgnoredType.REGEX(""))) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumSelectionStepContent(
    uiState: IgnoredSetupUiState,
    onAction: (IgnoredSetupAction) -> Unit
) {
    when (uiState.type) {
        is IgnoredType.SINGLE -> {
            SingleAlbumSelectionContent(
                uiState = uiState,
                onAction = onAction
            )
        }

        is IgnoredType.MULTIPLE -> {
            MultipleAlbumSelectionContent(
                uiState = uiState,
                onAction = onAction
            )
        }

        is IgnoredType.REGEX -> {
            RegexInputContent(
                uiState = uiState,
                onAction = onAction
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleAlbumSelectionContent(
    uiState: IgnoredSetupUiState,
    onAction: (IgnoredSetupAction) -> Unit
) {
    val selectedAlbum by remember(uiState.type) {
        derivedStateOf { (uiState.type as? IgnoredType.SINGLE)?.selectedAlbum }
    }
    val gridState = rememberLazyGridState()
    val columns = remember(uiState.albumGridSize) {
        Constants.albumCellsList[uiState.albumGridSize.coerceIn(0, Constants.albumCellsList.lastIndex)]
    }


    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(Res.string.setup_type_selection_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(Res.string.setup_type_selection_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    NavigationBackButton(
                        forcedAction = { onAction(IgnoredSetupAction.NavigateBack) }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SetupButton(
                    onClick = { onAction(IgnoredSetupAction.NavigateBack) },
                    modifier = Modifier.weight(1f),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    text = stringResource(Res.string.go_back)
                )
                SetupButton(
                    onClick = { onAction(IgnoredSetupAction.NavigateNext) },
                    enabled = uiState.canProceed,
                    modifier = Modifier.weight(1f),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    text = stringResource(Res.string.continue_string)
                )
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth(),
            columns = columns,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(
                items = uiState.albums,
                key = { item -> item.toString() }
            ) { album ->
                val isSelected = selectedAlbum?.id == album.id
                val isDisabled = uiState.ignoredAlbums.any { it.id == album.id }

                SelectableAlbumItem(
                    album = album,
                    isSelected = isSelected,
                    isDisabled = isDisabled,
                    showCheckmark = true,
                    onClick = {
                        if (!isDisabled) {
                            onAction(IgnoredSetupAction.SelectAlbum(if (isSelected) null else album))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultipleAlbumSelectionContent(
    uiState: IgnoredSetupUiState,
    onAction: (IgnoredSetupAction) -> Unit
) {
    val selectedAlbums by remember(uiState.selectedAlbums) {
        derivedStateOf { uiState.selectedAlbums }
    }
    val gridState = rememberLazyGridState()
    val columns = remember(uiState.albumGridSize) {
        Constants.albumCellsList[uiState.albumGridSize.coerceIn(0, Constants.albumCellsList.lastIndex)]
    }
    val continueText = stringResource(Res.string.continue_string)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(Res.string.setup_type_multiple_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(Res.string.setup_type_multiple_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    NavigationBackButton(
                        forcedAction = { onAction(IgnoredSetupAction.NavigateBack) }
                    )
                },
                actions = {
                    if (selectedAlbums.isNotEmpty()) {
                        Text(
                            text = stringResource(Res.string.selected_count, selectedAlbums.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SetupButton(
                    onClick = { onAction(IgnoredSetupAction.NavigateBack) },
                    modifier = Modifier.weight(1f),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    text = stringResource(Res.string.go_back)
                )
                SetupButton(
                    onClick = { onAction(IgnoredSetupAction.NavigateNext) },
                    enabled = uiState.canProceed,
                    modifier = Modifier.weight(1f),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    text = if (selectedAlbums.isNotEmpty()) {
                        stringResource(Res.string.continue_with_count, continueText, selectedAlbums.size)
                    } else continueText
                )
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth(),
            columns = columns,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(
                items = uiState.albums,
                key = { item -> item.toString() }
            ) { album ->
                val isSelected = selectedAlbums.any { it.id == album.id }
                val isDisabled = uiState.ignoredAlbums.any { it.id == album.id }

                SelectableAlbumItem(
                    album = album,
                    isSelected = isSelected,
                    isDisabled = isDisabled,
                    showCheckmark = true,
                    onClick = {
                        if (!isDisabled) {
                            onAction(IgnoredSetupAction.ToggleAlbum(album))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegexInputContent(
    uiState: IgnoredSetupUiState,
    onAction: (IgnoredSetupAction) -> Unit
) {
    val currentRegex = uiState.regex
    var localRegex by remember(currentRegex) { mutableStateOf(currentRegex) }
    var error by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val matchedAlbumsCount = remember(localRegex, uiState.albums) {
        try {
            localRegex.takeIf { it.isNotEmpty() }?.toRegex()
                ?.let { pattern -> uiState.albums.count { pattern.matchesAlbum(it) } }
                ?: 0
        } catch (e: Exception) {
            0
        }
    }

    val invalidRegex = stringResource(Res.string.setup_type_regex_error)
    val alreadyUsedRegex = stringResource(Res.string.setup_type_regex_error_second)

    LaunchedEffect(localRegex) {
        val validRegex = try {
            localRegex.toRegex()
            true
        } catch (e: Exception) {
            false
        }
        error = !validRegex
        if (error) {
            errorMessage = invalidRegex
        } else {
            error = uiState.ignoredAlbums.any { it.wildcard == localRegex }
            if (error) errorMessage = alreadyUsedRegex
        }

        if (!error && localRegex.isNotEmpty()) {
            errorMessage = ""
            onAction(IgnoredSetupAction.SetRegex(localRegex))
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(Res.string.setup_type_regex_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(Res.string.setup_type_regex_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    NavigationBackButton(
                        forcedAction = { onAction(IgnoredSetupAction.NavigateBack) }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SetupButton(
                    onClick = { onAction(IgnoredSetupAction.NavigateBack) },
                    modifier = Modifier.weight(1f),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    text = stringResource(Res.string.go_back)
                )
                SetupButton(
                    onClick = { onAction(IgnoredSetupAction.NavigateNext) },
                    enabled = localRegex.isNotEmpty() && !error,
                    modifier = Modifier.weight(1f),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    text = stringResource(Res.string.continue_string)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = localRegex,
                    onValueChange = { localRegex = it },
                    label = { Text(stringResource(Res.string.setup_type_regex_label)) },
                    placeholder = { Text(stringResource(Res.string.setup_type_regex_first_subtitle)) },
                    supportingText = when {
                        error && localRegex.isNotEmpty() -> {
                            {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        localRegex.isNotEmpty() -> {
                            {
                                Text(
                                    text = stringResource(
                                        Res.string.setup_album_regex_matched,
                                        matchedAlbumsCount
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (matchedAlbumsCount > 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                        }
                        else -> null
                    },
                    isError = error && localRegex.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.large
                        )
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = stringResource(Res.string.setup_type_regex_summary)
                )
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.setup_type_regex_example_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    RegexExample(
                        description = stringResource(Res.string.setup_type_regex_example_first_title),
                        pattern = stringResource(Res.string.setup_type_regex_first_subtitle)
                    )

                    RegexExample(
                        description = stringResource(Res.string.setup_type_regex_example_second_title),
                        pattern = stringResource(Res.string.setup_type_regex_example_second_subtitle)
                    )

                    RegexExample(
                        description = stringResource(Res.string.setup_type_regex_example_third_title),
                        pattern = stringResource(Res.string.setup_type_regex_example_third_subtitle)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmationStepContent(
    uiState: IgnoredSetupUiState,
    onAction: (IgnoredSetupAction) -> Unit
) {
    val albumsText = stringResource(Res.string.albums)
    val timelineText = stringResource(Res.string.timeline)
    val albumsAndTimelineText = stringResource(Res.string.albums_and_timeline)
    val unknownText = stringResource(Res.string.unknown_album)
    val singleAlbumText = stringResource(Res.string.setup_location_types_single)

    val locationText by remember(
        uiState.location,
        albumsText,
        timelineText,
        albumsAndTimelineText
    ) {
        derivedStateOf {
            when (uiState.location) {
                IgnoredAlbum.ALBUMS_ONLY -> albumsText
                IgnoredAlbum.TIMELINE_ONLY -> timelineText
                IgnoredAlbum.ALBUMS_AND_TIMELINE -> albumsAndTimelineText
                else -> unknownText
            }
        }
    }

    val typeText = when (val type = uiState.type) {
        is IgnoredType.SINGLE -> type.selectedAlbum?.label ?: singleAlbumText
        is IgnoredType.MULTIPLE -> stringResource(Res.string.albums_count, type.selectedAlbums.size)
        is IgnoredType.REGEX -> type.regex
    }

    val typeIcon by remember(uiState.type) {
        derivedStateOf {
            when (uiState.type) {
                is IgnoredType.SINGLE -> Icons.Outlined.PhotoAlbum
                is IgnoredType.MULTIPLE -> Icons.Outlined.FolderCopy
                is IgnoredType.REGEX -> GalleryIcons.RegularExpression
            }
        }
    }

    val noMatchesText = stringResource(Res.string.no_matches)
    val matchedText by remember(uiState.matchedAlbums, noMatchesText) {
        derivedStateOf {
            uiState.matchedAlbums.joinToString("\n") { it.label }.ifEmpty { noMatchesText }
        }
    }
    val extraMatchedText = if (uiState.matchedAlbums.size > 10) {
        stringResource(Res.string.matched_more_count, uiState.matchedAlbums.size - 10)
    } else null

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(Res.string.setup_confirmation_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(Res.string.setup_confirmation_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    NavigationBackButton(
                        forcedAction = { onAction(IgnoredSetupAction.NavigateBack) }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SetupButton(
                    onClick = { onAction(IgnoredSetupAction.NavigateBack) },
                    modifier = Modifier.weight(1f),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    text = stringResource(Res.string.go_back)
                )
                SetupButton(
                    onClick = { onAction(IgnoredSetupAction.Confirm) },
                    enabled = uiState.matchedAlbums.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    text = stringResource(Res.string.apply)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ConfirmationCard(
                    title = stringResource(Res.string.setup_confirmation_where),
                    value = locationText,
                    icon = Icons.Outlined.FilterNone
                )
            }

            item {
                ConfirmationCard(
                    title = stringResource(Res.string.setup_confirmation_who),
                    value = typeText,
                    icon = typeIcon
                )
            }

            item {
                ConfirmationCard(
                    title = stringResource(Res.string.setup_confirmation_matched),
                    value = matchedText,
                    icon = Icons.Outlined.Checklist,
                    extra = extraMatchedText
                )
            }
        }
    }
}

// ========== Previews ==========

private val mockAlbums = listOf(
    Album(
        id = 1,
        label = "Camera",
        uri = emptyUri,
        pathToThumbnail = "/DCIM/Camera/photo.jpg",
        relativePath = "DCIM/Camera",
        timestamp = 0,
        count = 42
    ),
    Album(
        id = 2,
        label = "Screenshots",
        uri = emptyUri,
        pathToThumbnail = "",
        relativePath = "Pictures/Screenshots",
        timestamp = 0,
        count = 156
    ),
    Album(
        id = 3,
        label = "Downloads",
        uri = emptyUri,
        pathToThumbnail = "",
        relativePath = "Download",
        timestamp = 0,
        count = 23
    ),
    Album(
        id = 4,
        label = "WhatsApp Images",
        uri = emptyUri,
        pathToThumbnail = "",
        relativePath = "Pictures/WhatsApp",
        timestamp = 0,
        count = 500
    ),
    Album(
        id = 5,
        label = "Telegram",
        uri = emptyUri,
        pathToThumbnail = "",
        relativePath = "Pictures/Telegram",
        timestamp = 0,
        count = 89
    )
)

@Preview
@Composable
private fun TypeSelectionStepSinglePreview() {
    IgnoredPreviewHost {
        IgnoredSetupSheetContent(
            uiState = IgnoredSetupUiState(
                currentStep = SetupStep.TYPE_SELECTION,
                location = IgnoredAlbum.ALBUMS_ONLY,
                type = IgnoredType.SINGLE(null)
            ),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun TypeSelectionStepMultiplePreview() {
    IgnoredPreviewHost {
        IgnoredSetupSheetContent(
            uiState = IgnoredSetupUiState(
                currentStep = SetupStep.TYPE_SELECTION,
                location = IgnoredAlbum.TIMELINE_ONLY,
                type = IgnoredType.MULTIPLE(emptyList())
            ),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun TypeSelectionStepRegexPreview() {
    IgnoredPreviewHost {
        IgnoredSetupSheetContent(
            uiState = IgnoredSetupUiState(
                currentStep = SetupStep.TYPE_SELECTION,
                location = IgnoredAlbum.ALBUMS_AND_TIMELINE,
                type = IgnoredType.REGEX("")
            ),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun SingleAlbumSelectionPreview() {
    IgnoredPreviewHost {
        IgnoredSetupSheetContent(
            uiState = IgnoredSetupUiState(
                currentStep = SetupStep.ALBUM_SELECTION,
                type = IgnoredType.SINGLE(mockAlbums[0]),
                albums = mockAlbums,
                canProceed = true
            ),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun MultipleAlbumSelectionPreview() {
    IgnoredPreviewHost {
        IgnoredSetupSheetContent(
            uiState = IgnoredSetupUiState(
                currentStep = SetupStep.ALBUM_SELECTION,
                type = IgnoredType.MULTIPLE(mockAlbums.take(3)),
                albums = mockAlbums,
                canProceed = true
            ),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun RegexInputEmptyPreview() {
    IgnoredPreviewHost {
        IgnoredSetupSheetContent(
            uiState = IgnoredSetupUiState(
                currentStep = SetupStep.ALBUM_SELECTION,
                type = IgnoredType.REGEX(""),
                albums = mockAlbums,
                canProceed = false
            ),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun RegexInputWithPatternPreview() {
    IgnoredPreviewHost {
        IgnoredSetupSheetContent(
            uiState = IgnoredSetupUiState(
                currentStep = SetupStep.ALBUM_SELECTION,
                type = IgnoredType.REGEX(".*Screenshots.*"),
                albums = mockAlbums,
                canProceed = true
            ),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun ConfirmationStepSinglePreview() {
    IgnoredPreviewHost {
        IgnoredSetupSheetContent(
            uiState = IgnoredSetupUiState(
                currentStep = SetupStep.CONFIRMATION,
                location = IgnoredAlbum.ALBUMS_ONLY,
                type = IgnoredType.SINGLE(mockAlbums[0]),
                matchedAlbums = listOf(mockAlbums[0])
            ),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun ConfirmationStepMultiplePreview() {
    IgnoredPreviewHost {
        IgnoredSetupSheetContent(
            uiState = IgnoredSetupUiState(
                currentStep = SetupStep.CONFIRMATION,
                location = IgnoredAlbum.TIMELINE_ONLY,
                type = IgnoredType.MULTIPLE(mockAlbums.take(3)),
                matchedAlbums = mockAlbums.take(3)
            ),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun ConfirmationStepRegexPreview() {
    IgnoredPreviewHost {
        val matchedAlbums = listOf(
            Album(
                id = 1,
                label = "Screenshots",
                uri = emptyUri,
                pathToThumbnail = "",
                relativePath = "Pictures/Screenshots",
                timestamp = 0,
                count = 100
            ),
            Album(
                id = 2,
                label = "Screenshot_2024",
                uri = emptyUri,
                pathToThumbnail = "",
                relativePath = "Pictures/Screenshot_2024",
                timestamp = 0,
                count = 50
            ),
            Album(
                id = 3,
                label = "Screenshots_old",
                uri = emptyUri,
                pathToThumbnail = "",
                relativePath = "Pictures/Screenshots_old",
                timestamp = 0,
                count = 30
            )
        )
        IgnoredSetupSheetContent(
            uiState = IgnoredSetupUiState(
                currentStep = SetupStep.CONFIRMATION,
                location = IgnoredAlbum.ALBUMS_AND_TIMELINE,
                type = IgnoredType.REGEX(".*Screenshots.*"),
                matchedAlbums = matchedAlbums
            ),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun ConfirmationStepNoMatchesPreview() {
    IgnoredPreviewHost {
        IgnoredSetupSheetContent(
            uiState = IgnoredSetupUiState(
                currentStep = SetupStep.CONFIRMATION,
                location = IgnoredAlbum.ALBUMS_ONLY,
                type = IgnoredType.REGEX("^NonExistent.*"),
                matchedAlbums = emptyList()
            ),
            onAction = {}
        )
    }
}
