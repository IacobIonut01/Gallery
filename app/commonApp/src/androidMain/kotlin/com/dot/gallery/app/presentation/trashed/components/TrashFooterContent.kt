/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.trashed.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.core.ui.components.SettingsItem
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.action_delete_all_permanently
import com.dot.gallery.generated.resources.trash
import com.dot.gallery.generated.resources.trash_deletion_warning
import com.dot.gallery.generated.resources.trash_restore_all
import org.jetbrains.compose.resources.stringResource

/**
 * The pure rendering of the trash footer — Restore all / Delete permanently
 * row (only while trash has items) and the auto-delete warning card. The
 * launcher/ViewModel/dialog wiring stays in [AutoDeleteFooter]; docs scenes
 * supply in-memory callbacks.
 */
@Composable
fun TrashFooterContent(
    hasMedia: Boolean,
    onRestoreAll: () -> Unit,
    onDeleteAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedVisibility(
            visible = hasMedia,
            enter = enterAnimation,
            exit = exitAnimation
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SetupButton(
                    modifier = Modifier.weight(1f),
                    onClick = onRestoreAll,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    text = stringResource(Res.string.trash_restore_all)
                )
                SetupButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDeleteAll,
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    text = stringResource(Res.string.action_delete_all_permanently)
                )
            }
        }

        SettingsItem(
            item = SettingsEntity.Preference(
                icon = Icons.Outlined.Info,
                title = stringResource(Res.string.trash),
                summary = stringResource(Res.string.trash_deletion_warning),
                screenPosition = Position.Alone,
            ),
            applyPaddings = false,
        )
    }
}
