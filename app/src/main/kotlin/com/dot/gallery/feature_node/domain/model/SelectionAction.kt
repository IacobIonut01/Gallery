/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EnhancedEncryption
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.graphics.vector.ImageVector
import com.dot.gallery.R
import kotlinx.serialization.Serializable

@Serializable
enum class ActionZone {
    TOP,
    MIDDLE,
    BOTTOM,
}

@Serializable
enum class ActionCondition {
    NONE,
    SUPPORTS_FAVORITES,
    IN_COLLECTION,
}

@Serializable
enum class SelectionAction(
    val labelRes: Int,
    val descriptionRes: Int,
    val zone: ActionZone,
    val minSelection: Int = 1,
    val maxSelection: Int = Int.MAX_VALUE,
    val requiresCondition: ActionCondition = ActionCondition.NONE,
) {
    // ── Top addon actions ──
    CLOSE(
        labelRes = R.string.selection_dialog_close_cd,
        descriptionRes = R.string.action_desc_close,
        zone = ActionZone.TOP,
    ),
    SELECT_ALL(
        labelRes = R.string.select_all,
        descriptionRes = R.string.action_desc_select_all,
        zone = ActionZone.TOP,
        minSelection = 0,
    ),
    INFO(
        labelRes = R.string.media_details,
        descriptionRes = R.string.action_desc_info,
        zone = ActionZone.TOP,
        maxSelection = 1,
    ),

    // ── Middle actions ──
    COLLECTION(
        labelRes = R.string.add_to_collection,
        descriptionRes = R.string.action_desc_collection,
        zone = ActionZone.MIDDLE,
    ),

    // ── Bottom bar actions ──
    SHARE(
        labelRes = R.string.share,
        descriptionRes = R.string.action_desc_share,
        zone = ActionZone.BOTTOM,
    ),
    FAVORITE(
        labelRes = R.string.favorite,
        descriptionRes = R.string.action_desc_favorite,
        zone = ActionZone.BOTTOM,
        requiresCondition = ActionCondition.SUPPORTS_FAVORITES,
    ),
    COPY(
        labelRes = R.string.copy,
        descriptionRes = R.string.action_desc_copy,
        zone = ActionZone.BOTTOM,
    ),
    MOVE(
        labelRes = R.string.move,
        descriptionRes = R.string.action_desc_move,
        zone = ActionZone.BOTTOM,
    ),
    TRASH(
        labelRes = R.string.trash,
        descriptionRes = R.string.action_desc_trash,
        zone = ActionZone.BOTTOM,
    ),
    ADD_TO_VAULT(
        labelRes = R.string.hide,
        descriptionRes = R.string.action_desc_hide,
        zone = ActionZone.BOTTOM,
    ),
    EDIT(
        labelRes = R.string.edit,
        descriptionRes = R.string.action_desc_edit,
        zone = ActionZone.BOTTOM,
        maxSelection = 1,
    ),
    ROTATE(
        labelRes = R.string.rotate,
        descriptionRes = R.string.action_desc_rotate,
        zone = ActionZone.BOTTOM,
    ),
    DOWNLOAD(
        labelRes = R.string.download,
        descriptionRes = R.string.action_desc_download,
        zone = ActionZone.BOTTOM,
    );

    val icon: ImageVector
        get() = when (this) {
            CLOSE -> Icons.Outlined.Close
            SELECT_ALL -> Icons.Outlined.SelectAll
            INFO -> Icons.Outlined.Info
            SHARE -> Icons.Outlined.Share
            FAVORITE -> Icons.Outlined.FavoriteBorder
            COLLECTION -> Icons.Outlined.Collections
            COPY -> Icons.Outlined.CopyAll
            MOVE -> Icons.AutoMirrored.Outlined.DriveFileMove
            TRASH -> Icons.Outlined.DeleteOutline
            ADD_TO_VAULT -> Icons.Outlined.EnhancedEncryption
            EDIT -> Icons.Outlined.Edit
            ROTATE -> Icons.AutoMirrored.Outlined.RotateRight
            DOWNLOAD -> Icons.Outlined.Download
        }
}

@Serializable
data class SelectionSheetConfig(
    val topActions: List<SelectionAction> = DEFAULT_TOP_ACTIONS,
    val middleActions: List<SelectionAction> = DEFAULT_MIDDLE_ACTIONS,
    val bottomActions: List<SelectionAction> = DEFAULT_BOTTOM_ACTIONS,
    val topActionsRightAligned: Boolean = false,
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
