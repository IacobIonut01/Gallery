/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.SetupButton
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.setup.generated.resources.Res
import com.dot.gallery.feature.setup.generated.resources.app_name
import com.dot.gallery.feature.setup.generated.resources.get_started
import com.dot.gallery.feature.setup.generated.resources.setup_welcome_subtitle
import com.dot.gallery.feature.setup.generated.resources.version_name_format


/**
 * Intro page of the setup wizard. [appIcon] is supplied by the shell so the launcher
 * artwork stays platform-side (mipmap drawable on Android).
 */
@Composable
fun SetupWelcomeContent(
    appIcon: Painter,
    versionName: String,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SetupWizardScaffold(
        modifier = modifier,
        showBack = false,
        showProgress = false,
        bottomBar = {
            SetupButton(
                text = stringResource(Res.string.get_started),
                applyHorizontalPadding = false,
                applyBottomPadding = false,
                applyInsets = false,
                applyNavigationPadding = false,
                onClick = onNext
            )
        }
    ) {
        Spacer(Modifier.height(48.dp))
        Image(
            painter = appIcon,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(Res.string.version_name_format, versionName),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            modifier = Modifier.padding(horizontal = 40.dp),
            text = stringResource(Res.string.setup_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(48.dp))
    }
}
