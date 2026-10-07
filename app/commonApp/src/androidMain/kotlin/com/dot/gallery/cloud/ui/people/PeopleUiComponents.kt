/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.people

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.request.ComposableImageRequest
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.core.ui.components.SettingsItem
import com.dot.gallery.core.ui.components.settings
import com.dot.gallery.feature.editor.adjustments.Crop
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.action_save
import com.dot.gallery.generated.resources.cloud_person_name_hint
import org.jetbrains.compose.resources.StringResource


/** Circular face avatar for a person, with a placeholder icon when there is no cover. */
@Composable
fun PersonAvatar(
    person: PersonInfo,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val thumbnailUrl = person.thumbnailUrl
    if (thumbnailUrl != null) {
        AsyncImage(
            request = ComposableImageRequest(thumbnailUrl),
            contentDescription = person.name,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Person, null,
                modifier = Modifier.size(size / 2),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Full-span section header styled like the settings list headers. */
@Composable
fun PeopleSectionHeader(title: String, count: Int) {
    SettingsItem(
        item = SettingsEntity.Header(title = "$title · $count"),
        applyPaddings = false
    )
}

/**
 * Modal bottom sheet for naming a person — shared by the list's inline "Add name"
 * and the detail screen's rename chip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenamePersonSheet(
    initialName: String,
    titleRes: StringResource,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    var name by remember { mutableStateOf(initialName) }
    val focusRequester = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(Res.string.cloud_person_name_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            SetupButton(
                text = stringResource(Res.string.action_save),
                applyHorizontalPadding = false,
                applyBottomPadding = false,
                applyInsets = false,
                onClick = {
                    onSave(name.trim())
                    onDismiss()
                }
            )
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
