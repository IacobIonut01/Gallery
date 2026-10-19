/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.core.LocalMediaSelector
import com.dot.gallery.core.ui.util.rememberedDerivedState
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItemHeader(
    modifier: Modifier = Modifier,
    date: String,
    location: String? = null,
    showAsBig: Boolean = false,
    bigHeaderOnly: Boolean = false,
    isChecked: MutableState<Boolean>,
    onChecked: (() -> Unit)? = null
) {
    val isCheckVisible by LocalMediaSelector.current.isSelectionActive.collectAsStateWithLifecycle()
    val smallModifier = remember(modifier) {
        modifier
            .padding(
                horizontal = 16.dp,
                vertical = 24.dp
            )
            .fillMaxWidth()
    }
    val bigModifier = remember(modifier, bigHeaderOnly) {
        modifier
            .padding(horizontal = 16.dp)
            .padding(top = 80.dp, bottom = if (bigHeaderOnly) 16.dp else 0.dp)
    }
    val bigTextStyle = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Bold
    )
    val smallTextStyle = MaterialTheme.typography.titleMedium
    val headerModifier by rememberedDerivedState(showAsBig) {
        if (showAsBig) bigModifier else smallModifier
    }
    val headerTextStyle by rememberedDerivedState(showAsBig) {
        if (showAsBig) bigTextStyle else smallTextStyle
    }
    Row(
        modifier = headerModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (!showAsBig) Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onLongClick = {
                            onChecked?.invoke()
                        },
                        onClick = {
                            if (isCheckVisible) onChecked?.invoke()
                        }
                    ) else Modifier
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = date,
                style = headerTextStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!location.isNullOrBlank()) {
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        AnimatedVisibility(
            visible = isCheckVisible && !showAsBig && onChecked != null,
            enter = enterAnimation,
            exit = exitAnimation
        ) {
            CheckBox(
                isChecked = isChecked.value,
                onCheck = onChecked,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}