/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.feature_node.presentation.settings.components.rememberSettingsFocusState
import com.dot.gallery.feature_node.presentation.settings.components.settingsFocusTarget
import com.dot.gallery.feature_node.presentation.util.maybeApply

@Composable
fun SetupButton(
    modifier: Modifier = Modifier,
    applyHorizontalPadding: Boolean = true,
    applyBottomPadding: Boolean = true,
    applyInsets: Boolean = true,
    applyNavigationPadding: Boolean = true,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp),
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    text: String,
    onClick: () -> Unit,
) {

    val layoutDirection = LocalLayoutDirection.current
    val focusState = rememberSettingsFocusState()
    val focusBorderWidth by animateDpAsState(
        targetValue = if (focusState.hasFocus) 3.dp else 0.dp,
        label = "setupButtonFocusBorderWidth"
    )
    val focusBorderColor by animateColorAsState(
        targetValue = if (focusState.hasFocus) MaterialTheme.colorScheme.onSurface else Color.Transparent,
        label = "setupButtonFocusBorderColor"
    )
    val displayCutoutInsets = WindowInsets.displayCutout.asPaddingValues()
    val horizontalDisplayCutoutInsets = PaddingValues(
        start = displayCutoutInsets.calculateStartPadding(layoutDirection),
        end = displayCutoutInsets.calculateEndPadding(layoutDirection)
    )
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            modifier = Modifier
                .settingsFocusTarget(focusState)
                .fillMaxWidth()
                .maybeApply(
                    condition = applyNavigationPadding,
                    modifier = Modifier.navigationBarsPadding()
                )
                .maybeApply(
                    condition = applyInsets,
                    modifier = Modifier.padding(paddingValues = horizontalDisplayCutoutInsets)
                )
                .maybeApply(
                    condition = applyBottomPadding,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                .maybeApply(
                    condition = applyHorizontalPadding,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                .height(64.dp)
                .border(focusBorderWidth, focusBorderColor, shape),
            onClick = onClick,
            shape = shape,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}
