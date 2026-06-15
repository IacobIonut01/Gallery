/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.settings.subsettings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dot.gallery.R
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.Settings.Misc.rememberSelectionSheetConfig
import com.dot.gallery.core.Settings.Misc.rememberShowSelectionTitles
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.feature_node.presentation.settings.components.SettingsItem
import com.dot.gallery.feature_node.domain.model.ActionZone
import com.dot.gallery.feature_node.domain.model.SelectionAction
import com.dot.gallery.feature_node.domain.model.SelectionSheetConfig
import com.dot.gallery.feature_node.presentation.mediaview.rememberedDerivedState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSelectionActionsScreen() {
    var config by rememberSelectionSheetConfig()
    val sanitizedConfig = remember(config) { config.sanitized() }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var showAddTopSheet by rememberSaveable { mutableStateOf(false) }
    var showAddMiddleSheet by rememberSaveable { mutableStateOf(false) }
    var showAddBottomSheet by rememberSaveable { mutableStateOf(false) }

    // Drag-to-reorder state
    var draggingZone by remember { mutableStateOf<ActionZone?>(null) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    // Approximate item height for swap threshold
    val itemHeightPx = remember(density) { with(density) { 72.dp.toPx() } }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.selection_actions)) },
                navigationIcon = { NavigationBackButton() },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = draggingZone == null,
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(LocalLayoutDirection.current),
                end = padding.calculateEndPadding(LocalLayoutDirection.current),
                top = 16.dp + padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Live Preview ──
            item(key = "preview") {
                Box(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    SelectionSheetPreview(config = sanitizedConfig)
                }
            }

            // ── Description ──
            item(key = "description") {
                Text(
                    text = stringResource(R.string.selection_actions_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp)
                )
            }

            // ── Reset to Defaults ──
            item(key = "reset") {
                Row(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = { config = SelectionSheetConfig() }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.reset_to_defaults))
                    }
                }
            }

            // ── Top Actions Header ──
            item(key = "top_header") {
                SectionHeader(
                    title = stringResource(R.string.top_actions),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 8.dp)
                )
            }

            // ── Right-align toggle ──
            item(key = "top_right_align") {
                SettingsItem(
                    item = SettingsEntity.SwitchPreference(
                        title = stringResource(R.string.top_actions_right_aligned),
                        summary = stringResource(R.string.top_actions_right_aligned_summary),
                        isChecked = sanitizedConfig.topActionsRightAligned,
                        onCheck = { checked ->
                            config = config.copy(topActionsRightAligned = checked)
                        },
                        screenPosition = Position.Alone
                    ),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            // ── Top Actions List ──
            itemsIndexed(
                items = sanitizedConfig.topActions,
                key = { _, action -> "top_${action.name}" }
            ) { index, action ->
                val isLocked = action == SelectionAction.CLOSE
                val position = itemPosition(index, sanitizedConfig.topActions.size)
                val isDragged = draggingZone == ActionZone.TOP && draggingIndex == index
                ActionListItem(
                    action = action,
                    position = position,
                    isLocked = isLocked,
                    canRemove = !isLocked,
                    isDragging = isDragged,
                    dragOffset = if (isDragged) dragOffsetY else 0f,
                    onDragStart = {
                        if (!isLocked) {
                            draggingZone = ActionZone.TOP
                            draggingIndex = index
                            dragOffsetY = 0f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDrag = { delta ->
                        if (draggingZone == ActionZone.TOP) {
                            dragOffsetY += delta
                            val swapThreshold = itemHeightPx * 0.5f
                            if (dragOffsetY > swapThreshold && draggingIndex < sanitizedConfig.topActions.lastIndex) {
                                val list = sanitizedConfig.topActions.toMutableList()
                                val item = list.removeAt(draggingIndex)
                                list.add(draggingIndex + 1, item)
                                config = config.copy(topActions = list)
                                draggingIndex++
                                dragOffsetY -= itemHeightPx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (dragOffsetY < -swapThreshold && draggingIndex > 0) {
                                val fromIdx = draggingIndex
                                if (sanitizedConfig.topActions[fromIdx] != SelectionAction.CLOSE &&
                                    sanitizedConfig.topActions[fromIdx - 1] != SelectionAction.CLOSE
                                ) {
                                    val list = sanitizedConfig.topActions.toMutableList()
                                    val item = list.removeAt(fromIdx)
                                    list.add(fromIdx - 1, item)
                                    config = config.copy(topActions = list)
                                    draggingIndex--
                                    dragOffsetY += itemHeightPx
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        draggingZone = null
                        draggingIndex = -1
                        dragOffsetY = 0f
                    },
                    onRemove = {
                        val list = sanitizedConfig.topActions.toMutableList()
                        list.removeAt(index)
                        config = config.copy(topActions = list)
                    },
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                )
            }

            // ── Add Top Action Button ──
            item(key = "add_top") {
                val availableTop = remember(sanitizedConfig) {
                    SelectionAction.entries
                        .filter { it.zone == ActionZone.TOP && it !in sanitizedConfig.topActions }
                }
                if (availableTop.isNotEmpty()) {
                    AddActionButton(
                        onClick = { showAddTopSheet = true },
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 4.dp, bottom = 16.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ── Middle Actions Header ──
            item(key = "middle_header") {
                SectionHeader(
                    title = stringResource(R.string.middle_actions),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 8.dp)
                )
            }

            // ── Middle Actions List ──
            itemsIndexed(
                items = sanitizedConfig.middleActions,
                key = { _, action -> "middle_${action.name}" }
            ) { index, action ->
                val position = itemPosition(index, sanitizedConfig.middleActions.size)
                val isDragged = draggingZone == ActionZone.MIDDLE && draggingIndex == index
                ActionListItem(
                    action = action,
                    position = position,
                    isLocked = false,
                    canRemove = true,
                    isDragging = isDragged,
                    dragOffset = if (isDragged) dragOffsetY else 0f,
                    onDragStart = {
                        draggingZone = ActionZone.MIDDLE
                        draggingIndex = index
                        dragOffsetY = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { delta ->
                        if (draggingZone == ActionZone.MIDDLE) {
                            dragOffsetY += delta
                            val swapThreshold = itemHeightPx * 0.5f
                            if (dragOffsetY > swapThreshold && draggingIndex < sanitizedConfig.middleActions.lastIndex) {
                                val list = sanitizedConfig.middleActions.toMutableList()
                                val item = list.removeAt(draggingIndex)
                                list.add(draggingIndex + 1, item)
                                config = config.copy(middleActions = list)
                                draggingIndex++
                                dragOffsetY -= itemHeightPx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (dragOffsetY < -swapThreshold && draggingIndex > 0) {
                                val list = sanitizedConfig.middleActions.toMutableList()
                                val item = list.removeAt(draggingIndex)
                                list.add(draggingIndex - 1, item)
                                config = config.copy(middleActions = list)
                                draggingIndex--
                                dragOffsetY += itemHeightPx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    },
                    onDragEnd = {
                        draggingZone = null
                        draggingIndex = -1
                        dragOffsetY = 0f
                    },
                    onRemove = {
                        val list = sanitizedConfig.middleActions.toMutableList()
                        list.removeAt(index)
                        config = config.copy(middleActions = list)
                    },
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                )
            }

            // ── Add Middle Action Button ──
            item(key = "add_middle") {
                val availableMiddle = remember(sanitizedConfig) {
                    SelectionAction.entries
                        .filter { it.zone == ActionZone.MIDDLE && it !in sanitizedConfig.middleActions }
                }
                if (availableMiddle.isNotEmpty()) {
                    AddActionButton(
                        onClick = { showAddMiddleSheet = true },
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 4.dp, bottom = 16.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ── Bottom Actions Header ──
            item(key = "bottom_header") {
                SectionHeader(
                    title = stringResource(R.string.action_bar),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 8.dp)
                )
            }

            // ── Bottom Actions List ──
            itemsIndexed(
                items = sanitizedConfig.bottomActions,
                key = { _, action -> "bottom_${action.name}" }
            ) { index, action ->
                val position = itemPosition(index, sanitizedConfig.bottomActions.size)
                val isDragged = draggingZone == ActionZone.BOTTOM && draggingIndex == index
                ActionListItem(
                    action = action,
                    position = position,
                    isLocked = false,
                    canRemove = sanitizedConfig.bottomActions.size > 1,
                    isDragging = isDragged,
                    dragOffset = if (isDragged) dragOffsetY else 0f,
                    onDragStart = {
                        draggingZone = ActionZone.BOTTOM
                        draggingIndex = index
                        dragOffsetY = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { delta ->
                        if (draggingZone == ActionZone.BOTTOM) {
                            dragOffsetY += delta
                            val swapThreshold = itemHeightPx * 0.5f
                            if (dragOffsetY > swapThreshold && draggingIndex < sanitizedConfig.bottomActions.lastIndex) {
                                val list = sanitizedConfig.bottomActions.toMutableList()
                                val item = list.removeAt(draggingIndex)
                                list.add(draggingIndex + 1, item)
                                config = config.copy(bottomActions = list)
                                draggingIndex++
                                dragOffsetY -= itemHeightPx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (dragOffsetY < -swapThreshold && draggingIndex > 0) {
                                val list = sanitizedConfig.bottomActions.toMutableList()
                                val item = list.removeAt(draggingIndex)
                                list.add(draggingIndex - 1, item)
                                config = config.copy(bottomActions = list)
                                draggingIndex--
                                dragOffsetY += itemHeightPx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    },
                    onDragEnd = {
                        draggingZone = null
                        draggingIndex = -1
                        dragOffsetY = 0f
                    },
                    onRemove = {
                        if (sanitizedConfig.bottomActions.size > 1) {
                            val list = sanitizedConfig.bottomActions.toMutableList()
                            list.removeAt(index)
                            config = config.copy(bottomActions = list)
                        }
                    },
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                )
            }

            // ── Add Bottom Action Button ──
            item(key = "add_bottom") {
                val availableBottom = remember(sanitizedConfig) {
                    SelectionAction.entries
                        .filter { it.zone == ActionZone.BOTTOM && it !in sanitizedConfig.bottomActions }
                }
                if (availableBottom.isNotEmpty()) {
                    AddActionButton(
                        onClick = { showAddBottomSheet = true },
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 4.dp, bottom = 16.dp)
                    )
                }
            }
        }
    }

    // ── Add Action Sheets ──
    if (showAddTopSheet) {
        val availableTop = remember(sanitizedConfig) {
            SelectionAction.entries
                .filter { it.zone == ActionZone.TOP && it !in sanitizedConfig.topActions }
        }
        AddActionSheet(
            actions = availableTop,
            onActionSelected = { action ->
                config = config.copy(topActions = config.topActions + action)
                showAddTopSheet = false
            },
            onDismiss = { showAddTopSheet = false }
        )
    }

    if (showAddMiddleSheet) {
        val availableMiddle = remember(sanitizedConfig) {
            SelectionAction.entries
                .filter { it.zone == ActionZone.MIDDLE && it !in sanitizedConfig.middleActions }
        }
        AddActionSheet(
            actions = availableMiddle,
            onActionSelected = { action ->
                config = config.copy(middleActions = config.middleActions + action)
                showAddMiddleSheet = false
            },
            onDismiss = { showAddMiddleSheet = false }
        )
    }

    if (showAddBottomSheet) {
        val availableBottom = remember(sanitizedConfig) {
            SelectionAction.entries
                .filter { it.zone == ActionZone.BOTTOM && it !in sanitizedConfig.bottomActions }
        }
        AddActionSheet(
            actions = availableBottom,
            onActionSelected = { action ->
                config = config.copy(bottomActions = config.bottomActions + action)
                showAddBottomSheet = false
            },
            onDismiss = { showAddBottomSheet = false }
        )
    }
}

private fun itemPosition(index: Int, size: Int): Position {
    return when {
        size == 1 -> Position.Alone
        index == 0 -> Position.Top
        index == size - 1 -> Position.Bottom
        else -> Position.Middle
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Live Preview — exact same dimensions/styling as SelectionSheet
// Uses solid background (non-blur fallback)
// ────────────────────────────────────────────────────────────────────────────────

@Composable
internal fun SelectionSheetPreview(
    config: SelectionSheetConfig,
    showTitlesOverride: Boolean? = null,
) {
    val showTitlesSetting by rememberShowSelectionTitles()
    val showTitles = showTitlesOverride ?: showTitlesSetting
    val tintColor = MaterialTheme.colorScheme.onSurface
    val barSurfaceColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    val pillShape = RoundedCornerShape(28.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top row: addon pills — matching SelectionAddon, scrollable with fade
        val topScrollState = rememberScrollState()
        val rightAligned = config.topActionsRightAligned
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fadeWidth = 32.dp.toPx()
                    if (topScrollState.canScrollForward) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                startX = size.width - fadeWidth,
                                endX = size.width
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
        ) {
            if (rightAligned) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // CLOSE stays on the left
                    if (SelectionAction.CLOSE in config.topActions) {
                        Row(
                            modifier = Modifier
                                .background(
                                    color = barSurfaceColor,
                                    shape = pillShape
                                )
                                .clip(pillShape)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                modifier = Modifier.size(24.dp),
                                imageVector = Icons.Outlined.Close,
                                colorFilter = ColorFilter.tint(tintColor),
                                contentDescription = null
                            )
                            Text(
                                text = "3",
                                style = MaterialTheme.typography.titleMedium,
                                color = tintColor
                            )
                        }
                    }
                    // Remaining actions pushed to the right, scrollable
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(topScrollState),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        config.topActions.forEach { action ->
                            if (action != SelectionAction.CLOSE) {
                                TopActionPreviewPill(
                                    action = action,
                                    tintColor = tintColor,
                                    barSurfaceColor = barSurfaceColor,
                                    pillShape = pillShape
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(topScrollState),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    config.topActions.forEach { action ->
                        TopActionPreviewPill(
                            action = action,
                            tintColor = tintColor,
                            barSurfaceColor = barSurfaceColor,
                            pillShape = pillShape
                        )
                    }
                }
            }
        }
        // Middle: full-width pill buttons
        config.middleActions.forEach { action ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = barSurfaceColor,
                        shape = pillShape
                    )
                    .clip(pillShape)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    modifier = Modifier.size(24.dp),
                    imageVector = action.icon,
                    colorFilter = ColorFilter.tint(tintColor),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = stringResource(action.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = tintColor,
                    maxLines = 1,
                )
            }
        }
        // Bottom: action bar — matching SelectionBarColumn, scrollable
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = barSurfaceColor,
                    shape = pillShape
                )
                .clip(pillShape)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (config.bottomActions.isEmpty()) {
                Text(
                    text = stringResource(R.string.minimum_one_action),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tintColor.copy(alpha = 0.5f),
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                config.bottomActions.forEach { action ->
                    Column(
                        modifier = Modifier
                            .defaultMinSize(
                                minHeight = if (showTitles) 80.dp else 64.dp
                            )
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .padding(top = 12.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            imageVector = action.icon,
                            colorFilter = ColorFilter.tint(tintColor),
                            contentDescription = null,
                            modifier = Modifier.height(32.dp)
                        )
                        if (showTitles) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(action.labelRes),
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium,
                                color = tintColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopActionPreviewPill(
    action: SelectionAction,
    tintColor: Color,
    barSurfaceColor: Color,
    pillShape: RoundedCornerShape,
) {
    Row(
        modifier = Modifier
            .background(
                color = barSurfaceColor,
                shape = pillShape
            )
            .clip(pillShape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.size(24.dp),
            imageVector = action.icon,
            colorFilter = ColorFilter.tint(tintColor),
            contentDescription = null
        )
        Text(
            text = when (action) {
                SelectionAction.CLOSE -> "3"
                else -> stringResource(action.labelRes)
            },
            style = MaterialTheme.typography.titleMedium,
            color = tintColor
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Action List Item — matches SettingsItem design: animated corner radii,
// Position-based shapes, surfaceContainer bg, swipe-to-delete, drag handle
// ────────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionListItem(
    action: SelectionAction,
    position: Position,
    isLocked: Boolean,
    canRemove: Boolean = true,
    isDragging: Boolean = false,
    dragOffset: Float = 0f,
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainer

    val fullCornerRadius by animateDpAsState(
        targetValue = 24.dp,
        label = "fullCornerRadius"
    )
    val normalCornerRadius by animateDpAsState(
        targetValue = 8.dp,
        label = "normalCornerRadius"
    )

    val shape by rememberedDerivedState(position, fullCornerRadius, normalCornerRadius) {
        when (position) {
            Position.Alone -> RoundedCornerShape(fullCornerRadius)
            Position.Top -> RoundedCornerShape(
                topStart = fullCornerRadius,
                topEnd = fullCornerRadius,
                bottomStart = normalCornerRadius,
                bottomEnd = normalCornerRadius
            )
            Position.Middle -> RoundedCornerShape(normalCornerRadius)
            Position.Bottom -> RoundedCornerShape(
                topStart = normalCornerRadius,
                topEnd = normalCornerRadius,
                bottomStart = fullCornerRadius,
                bottomEnd = fullCornerRadius
            )
        }
    }

    val paddingModifier = when (position) {
        Position.Alone -> Modifier.padding(bottom = 16.dp)
        Position.Bottom -> Modifier.padding(top = 1.dp, bottom = 16.dp)
        Position.Middle -> Modifier.padding(vertical = 1.dp)
        Position.Top -> Modifier.padding(bottom = 1.dp)
    }

    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 8f else 0f,
        label = "dragElevation"
    )

    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val dragModifier = if (!isLocked) {
        Modifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { currentOnDragStart() },
                onDrag = { change, offset ->
                    change.consume()
                    currentOnDrag(offset.y)
                },
                onDragEnd = { currentOnDragEnd() },
                onDragCancel = { currentOnDragEnd() }
            )
        }
    } else Modifier

    val itemContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = modifier
                    .padding(horizontal = 16.dp)
                    .clip(shape)
                    .background(color = backgroundColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Drag handle
                    Icon(
                        Icons.Outlined.DragHandle, null,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(22.dp),
                        tint = if (isDragging) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )

                    // Action icon
                    Image(
                        imageVector = action.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(22.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                    )

                    // Label + description
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(action.labelRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(action.descriptionRes),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Locked indicator
                    if (isLocked) {
                        Icon(
                            Icons.Outlined.Lock, stringResource(R.string.action_locked),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }

    // Outer wrapper: drag gesture sits above SwipeToDismissBox so it isn't intercepted
    Box(
        modifier = Modifier
            .then(paddingModifier)
            .graphicsLayer {
                translationY = dragOffset
                shadowElevation = elevation
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
            }
            .then(dragModifier)
    ) {
        if (!isLocked && canRemove && !isDragging) {
            val dismissState = rememberSwipeToDismissBoxState(
                positionalThreshold = { totalDistance -> totalDistance * 0.4f }
            )
            LaunchedEffect(dismissState.targetValue) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    onRemove()
                }
            }
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(shape)
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                enableDismissFromStartToEnd = false,
                enableDismissFromEndToStart = true,
                content = { itemContent() }
            )
        } else {
            itemContent()
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Add Action Button — matches SettingsItem Alone position style
// ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun AddActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.Add, null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.add_action),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Section Header — matches SettingsEntity.Header style
// ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

// ────────────────────────────────────────────────────────────────────────────────
// Add Action Bottom Sheet — picker for available actions
// ────────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddActionSheet(
    actions: List<SelectionAction>,
    onActionSelected: (SelectionAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.add_action),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            if (actions.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_actions_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            } else {
                actions.forEachIndexed { index, action ->
                    val position = itemPosition(index, actions.size)
                    AddActionSheetItem(
                        action = action,
                        position = position,
                        onClick = { onActionSelected(action) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddActionSheetItem(
    action: SelectionAction,
    position: Position,
    onClick: () -> Unit,
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainer

    val fullCornerRadius = 24.dp
    val normalCornerRadius = 8.dp

    val shape = when (position) {
        Position.Alone -> RoundedCornerShape(fullCornerRadius)
        Position.Top -> RoundedCornerShape(
            topStart = fullCornerRadius, topEnd = fullCornerRadius,
            bottomStart = normalCornerRadius, bottomEnd = normalCornerRadius
        )
        Position.Middle -> RoundedCornerShape(normalCornerRadius)
        Position.Bottom -> RoundedCornerShape(
            topStart = normalCornerRadius, topEnd = normalCornerRadius,
            bottomStart = fullCornerRadius, bottomEnd = fullCornerRadius
        )
    }

    val paddingModifier = when (position) {
        Position.Alone -> Modifier.padding(bottom = 0.dp)
        Position.Bottom -> Modifier.padding(top = 1.dp)
        Position.Middle -> Modifier.padding(vertical = 1.dp)
        Position.Top -> Modifier.padding(bottom = 1.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(paddingModifier)
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(color = backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(22.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(action.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(action.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
