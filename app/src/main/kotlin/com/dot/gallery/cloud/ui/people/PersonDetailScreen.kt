/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.people

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
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
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.feature_node.domain.model.MediaMetadataState
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
    var showRenameSheet by remember { mutableStateOf(false) }
    var editNameText by remember { mutableStateOf("") }
    var showBirthdayPicker by remember { mutableStateOf(false) }

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
                onRenameClick = {
                    editNameText = state.person?.name ?: ""
                    showRenameSheet = true
                },
                onBirthdayClick = { showBirthdayPicker = true }
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
    onRenameClick: () -> Unit,
    onBirthdayClick: () -> Unit
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
    }
}
