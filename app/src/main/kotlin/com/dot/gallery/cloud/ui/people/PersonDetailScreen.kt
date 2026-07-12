/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.people

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Merge
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.dot.gallery.R
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.navigate
import com.dot.gallery.core.navigateUp
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.feature_node.domain.model.MediaMetadataState
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.presentation.common.MediaScreen
import com.dot.gallery.feature_node.presentation.util.Screen

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalGlideComposeApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun PersonDetailScreen(
    metadataState: State<MediaMetadataState>,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    val viewModel = hiltViewModel<PersonDetailViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mediaState = viewModel.mediaState.collectAsStateWithLifecycle()
    val blurProgress by viewModel.blurProgress.collectAsStateWithLifecycle()
    val mergeCandidates by viewModel.mergeCandidates.collectAsStateWithLifecycle()
    val personMedia by viewModel.personMedia.collectAsStateWithLifecycle()
    var showRenameSheet by remember { mutableStateOf(false) }
    var editNameText by remember { mutableStateOf("") }
    var showBirthdayPicker by remember { mutableStateOf(false) }
    var showBlurDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var showCoverDialog by remember { mutableStateOf(false) }

    val personName = state.person?.name?.ifBlank {
        stringResource(R.string.cloud_people_unknown)
    } ?: stringResource(R.string.cloud_person_detail_title)

    val eventHandler = LocalEventHandler.current

    MediaScreen(
        albumName = personName,
        customDateHeader = stringResource(R.string.cloud_person_photo_count, mediaState.value.media.size),
        mediaState = mediaState,
        metadataState = metadataState,
        target = "person_${state.person?.id}",
        customViewingNavigation = state.person?.id?.let { personId ->
            { media ->
                eventHandler.navigate(Screen.MediaViewScreen.idAndPerson(media.id, personId))
            }
        },
        navActionsContent = { _, _ -> },
        aboveGridContent = {
            PersonHeader(
                state = state,
                isLocalPerson = viewModel.isLocalPerson,
                blurProgress = blurProgress,
                onRenameClick = {
                    editNameText = state.person?.name ?: ""
                    showRenameSheet = true
                },
                onBirthdayClick = { showBirthdayPicker = true },
                onHideClick = { viewModel.hidePerson { eventHandler.navigateUp() } },
                onBlurEverywhereClick = { showBlurDialog = true },
                canMerge = mergeCandidates.isNotEmpty(),
                onMergeClick = { showMergeDialog = true },
                onSetCoverClick = { showCoverDialog = true }
            )
        },
        onActivityResult = { },
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope,
    )

    // Rename bottom sheet with IME padding
    if (showRenameSheet) {
        val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
        val focusRequester = remember { FocusRequester() }

        ModalBottomSheet(
            onDismissRequest = { showRenameSheet = false },
            sheetState = sheetState,
            modifier = Modifier.imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.cloud_person_edit_name),
                    style = MaterialTheme.typography.titleLarge
                )
                OutlinedTextField(
                    value = editNameText,
                    onValueChange = { editNameText = it },
                    placeholder = { Text(stringResource(R.string.cloud_person_name_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                SetupButton(
                    text = stringResource(R.string.action_save),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    onClick = {
                        viewModel.updateName(editNameText)
                        showRenameSheet = false
                    }
                )
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    // Birthday date picker dialog
    if (showBirthdayPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseBirthDateMillis(state.person?.birthDate)
        )
        DatePickerDialog(
            onDismissRequest = { showBirthdayPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatted = formatBirthDate(millis)
                        viewModel.updateBirthDate(formatted)
                    }
                    showBirthdayPicker = false
                }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBirthdayPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Choose blur vs mosaic for the "blur everywhere" batch.
    if (showBlurDialog) {
        ModalBottomSheet(onDismissRequest = { showBlurDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.cloud_person_blur_everywhere),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.cloud_person_blur_everywhere_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                SetupButton(
                    text = stringResource(R.string.type_blur),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    onClick = {
                        viewModel.blurEverywhere(useMosaic = false)
                        showBlurDialog = false
                    }
                )
                SetupButton(
                    text = stringResource(R.string.type_mosaic),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    onClick = {
                        viewModel.blurEverywhere(useMosaic = true)
                        showBlurDialog = false
                    }
                )
            }
        }
    }

    // Merge into another on-device person.
    if (showMergeDialog) {
        ModalBottomSheet(onDismissRequest = { showMergeDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.cloud_person_merge),
                    style = MaterialTheme.typography.titleLarge
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(96.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mergeCandidates, key = { it.id }) { candidate ->
                        Column(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    viewModel.mergeInto(candidate.id)
                                    showMergeDialog = false
                                    eventHandler.navigateUp()
                                }
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (candidate.thumbnailUrl != null) {
                                GlideImage(
                                    model = candidate.thumbnailUrl.toUri(),
                                    contentDescription = candidate.name,
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Person, null,
                                        modifier = Modifier.size(44.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = candidate.name.ifBlank {
                                    stringResource(R.string.cloud_people_unknown)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    // Choose a new cover face from this person's photos.
    if (showCoverDialog) {
        ModalBottomSheet(onDismissRequest = { showCoverDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.cloud_person_set_cover),
                    style = MaterialTheme.typography.titleLarge
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(96.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(personMedia, key = { it.id }) { media ->
                        GlideImage(
                            model = media.getUri(),
                            contentDescription = null,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    viewModel.setCover(media)
                                    showCoverDialog = false
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

private fun parseBirthDateMillis(birthDate: String?): Long? {
    if (birthDate.isNullOrBlank()) return null
    return try {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(birthDate)?.time
    } catch (_: Exception) { null }
}

private fun formatBirthDate(millis: Long): String {
    return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(millis))
}

private fun formatBirthDateDisplay(birthDate: String): String {
    return try {
        val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(birthDate)
        parsed?.let { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(it) } ?: birthDate
    } catch (_: Exception) { birthDate }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun PersonHeader(
    state: PersonDetailUiState,
    isLocalPerson: Boolean = false,
    blurProgress: Pair<Int, Int>? = null,
    onRenameClick: () -> Unit,
    onBirthdayClick: () -> Unit,
    onHideClick: () -> Unit = {},
    onBlurEverywhereClick: () -> Unit = {},
    canMerge: Boolean = false,
    onMergeClick: () -> Unit = {},
    onSetCoverClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val thumbnailUrl = state.person?.thumbnailUrl
        if (thumbnailUrl != null) {
            GlideImage(
                model = thumbnailUrl.toUri(),
                contentDescription = state.person.name,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person, null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChip(
                onClick = onRenameClick,
                label = {
                    Text(
                        text = state.person?.name?.ifBlank { stringResource(R.string.cloud_person_add_name) }
                            ?: stringResource(R.string.cloud_person_add_name)
                    )
                },
                icon = {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            SuggestionChip(
                onClick = onBirthdayClick,
                label = {
                    Text(
                        text = state.person?.birthDate?.let { formatBirthDateDisplay(it) }
                            ?: stringResource(R.string.cloud_person_add_birthday)
                    )
                },
                icon = {
                    Icon(
                        Icons.Outlined.Cake,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        // On-device person management actions (hide + blur everywhere).
        if (isLocalPerson) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = onHideClick,
                    label = { Text(stringResource(R.string.cloud_person_hide)) },
                    icon = {
                        Icon(
                            Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        iconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                SuggestionChip(
                    onClick = onBlurEverywhereClick,
                    label = { Text(stringResource(R.string.cloud_person_blur_everywhere)) },
                    icon = {
                        Icon(
                            Icons.Outlined.BlurOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        iconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = onSetCoverClick,
                    label = { Text(stringResource(R.string.cloud_person_set_cover)) },
                    icon = {
                        Icon(
                            Icons.Outlined.Image,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        iconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                if (canMerge) {
                    SuggestionChip(
                        onClick = onMergeClick,
                        label = { Text(stringResource(R.string.cloud_person_merge)) },
                        icon = {
                            Icon(
                                Icons.Outlined.Merge,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            iconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            if (blurProgress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.cloud_person_blurring_progress,
                        blurProgress.first,
                        blurProgress.second
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
