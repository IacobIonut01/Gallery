/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import kotlinx.serialization.Serializable

@Serializable
data class SelectionSheetConfig(
    val topActions: List<SelectionAction> = DEFAULT_TOP_ACTIONS,
    val middleActions: List<SelectionAction> = DEFAULT_MIDDLE_ACTIONS,
    val bottomActions: List<SelectionAction> = DEFAULT_BOTTOM_ACTIONS,
    val topActionsRightAligned: Boolean = false,
    val fillActionBar: Boolean = false,
) {
    fun sanitized(): SelectionSheetConfig {
        val top = topActions.distinct().filter { it.zone == ActionZone.TOP }.toMutableList()
        if (SelectionAction.CLOSE !in top) {
            top.add(0, SelectionAction.CLOSE)
        }
        val middle = middleActions.distinct().filter { it.zone == ActionZone.MIDDLE }
        val bottom = bottomActions.distinct().filter { it.zone == ActionZone.BOTTOM }
        return SelectionSheetConfig(
            topActions = top,
            middleActions = middle,
            bottomActions = bottom,
            topActionsRightAligned = topActionsRightAligned,
            fillActionBar = fillActionBar,
        )
    }

    companion object {
        val DEFAULT_TOP_ACTIONS = listOf(
            SelectionAction.CLOSE,
            SelectionAction.SELECT_ALL,
        )
        val DEFAULT_MIDDLE_ACTIONS = emptyList<SelectionAction>()
        val DEFAULT_BOTTOM_ACTIONS = listOf(
            SelectionAction.SHARE,
            SelectionAction.FAVORITE,
            SelectionAction.COPY,
            SelectionAction.MOVE,
            SelectionAction.TRASH,
        )
    }
}
