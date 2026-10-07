/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.core.ui.settings.rememberPreference
import com.dot.gallery.domain.settings.SettingsKeys
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.setup.generated.resources.Res
import com.dot.gallery.feature.setup.generated.resources.setup_appearance_summary
import com.dot.gallery.feature.setup.generated.resources.setup_continue
import com.dot.gallery.feature.setup.generated.resources.setup_looks_restart_note
import com.dot.gallery.feature.setup.generated.resources.setup_looks_subtitle
import com.dot.gallery.feature.setup.generated.resources.setup_looks_title
import com.dot.gallery.feature.setup.generated.resources.setup_open_appearance
import com.dot.gallery.feature.setup.generated.resources.setup_section_app_logo
import com.dot.gallery.feature.setup.generated.resources.setup_section_app_name
import com.dot.gallery.feature.setup.generated.resources.setup_section_appearance


/**
 * Looks & Feel page: app-name and app-logo pickers plus an appearance-settings shortcut.
 * Alias selection persists through `SettingsKeys.appNameAlias`/`appLogoAlias`; the app
 * applies them on finish (restart when the launcher alias actually changes).
 *
 * [logoPainter] resolves each alias to its launcher artwork — mipmap drawables on
 * Android, so the seam stays platform-side.
 */
@Composable
fun SetupLooksFeelContent(
    state: SetupLooksFeelState,
    actions: SetupLooksFeelActions,
    logoPainter: @Composable (logoAlias: String) -> Painter,
    stepNumber: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var appNameAlias by rememberPreference(SettingsKeys.appNameAlias)
    var appLogoAlias by rememberPreference(SettingsKeys.appLogoAlias)

    SetupWizardScaffold(
        modifier = modifier,
        showBack = true,
        onBack = onBack,
        stepNumber = stepNumber,
        totalSteps = totalSteps,
        title = stringResource(Res.string.setup_looks_title),
        subtitle = stringResource(Res.string.setup_looks_subtitle),
        bottomBar = {
            SetupButton(
                text = stringResource(Res.string.setup_continue),
                applyHorizontalPadding = false,
                applyBottomPadding = false,
                applyInsets = false,
                applyNavigationPadding = false,
                onClick = onNext
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App name
            SetupSectionCard(title = stringResource(Res.string.setup_section_app_name)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.nameAliases.forEach { alias ->
                        LogoChoice(
                            modifier = Modifier.weight(1f),
                            label = alias,
                            iconPainter = logoPainter(appLogoAlias),
                            selected = appNameAlias == alias,
                            onClick = { appNameAlias = alias }
                        )
                    }
                }
            }

            // App logo
            SetupSectionCard(title = stringResource(Res.string.setup_section_app_logo)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.logoAliases.forEach { alias ->
                        LogoChoice(
                            modifier = Modifier.width(96.dp),
                            label = alias,
                            iconPainter = logoPainter(alias),
                            selected = appLogoAlias == alias,
                            onClick = { appLogoAlias = alias }
                        )
                    }
                }
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = stringResource(Res.string.setup_looks_restart_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Appearance
            SetupSectionCard(
                title = stringResource(Res.string.setup_section_appearance),
                subtitle = stringResource(Res.string.setup_appearance_summary)
            ) {
                SetupButton(
                    text = stringResource(Res.string.setup_open_appearance),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    applyNavigationPadding = false,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = actions.onOpenAppearance
                )
            }
        }
    }
}

@Composable
private fun LogoChoice(
    label: String,
    iconPainter: Painter,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    else Color.Transparent
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = iconPainter,
            contentDescription = label,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
