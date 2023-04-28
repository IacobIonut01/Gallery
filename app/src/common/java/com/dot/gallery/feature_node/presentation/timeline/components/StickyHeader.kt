/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.timeline.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StickyHeader(
    date: String,
    showAsBig: Boolean = false,
    isCheckVisible: MutableState<Boolean>,
    isChecked: MutableState<Boolean>,
    onChecked: () -> Unit
) {
    val smallModifier = Modifier
        .padding(
            horizontal = 16.dp,
            vertical = 24.dp
        )
        .fillMaxWidth()
    val bigModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 80.dp)
    val bigTextStyle = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Bold
    )
    val smallTextStyle = MaterialTheme.typography.titleMedium
    Row(
        modifier = if (showAsBig) bigModifier else smallModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date,
            style = if (showAsBig) bigTextStyle else smallTextStyle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.then(
                if (!showAsBig) Modifier.combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onLongClick = {
                        onChecked()
                    },
                    onClick = {
                        if (isCheckVisible.value) onChecked()
                    }
                ) else Modifier
            )
        )
        if (!showAsBig) {
            AnimatedVisibility(
                visible = isCheckVisible.value,
                enter = enterAnimation,
                exit = exitAnimation
            ) {
                RadioButton(
                    modifier = Modifier.height(22.dp),
                    selected = isChecked.value,
                    onClick = onChecked
                )
            }
        }
    }
}