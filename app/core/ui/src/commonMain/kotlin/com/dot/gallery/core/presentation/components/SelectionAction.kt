/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EnhancedEncryption
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import com.dot.gallery.core.ui.generated.resources.Res
import com.dot.gallery.core.ui.generated.resources.action_desc_close
import com.dot.gallery.core.ui.generated.resources.action_desc_collection
import com.dot.gallery.core.ui.generated.resources.action_desc_copy
import com.dot.gallery.core.ui.generated.resources.action_desc_download
import com.dot.gallery.core.ui.generated.resources.action_desc_edit
import com.dot.gallery.core.ui.generated.resources.action_desc_favorite
import com.dot.gallery.core.ui.generated.resources.action_desc_hide
import com.dot.gallery.core.ui.generated.resources.action_desc_info
import com.dot.gallery.core.ui.generated.resources.action_desc_move
import com.dot.gallery.core.ui.generated.resources.action_desc_move_to_private_folder
import com.dot.gallery.core.ui.generated.resources.action_desc_rotate
import com.dot.gallery.core.ui.generated.resources.action_desc_select_all
import com.dot.gallery.core.ui.generated.resources.action_desc_set_as_album_cover
import com.dot.gallery.core.ui.generated.resources.action_desc_share
import com.dot.gallery.core.ui.generated.resources.action_desc_trash
import com.dot.gallery.core.ui.generated.resources.add_to_collection
import com.dot.gallery.core.ui.generated.resources.copy
import com.dot.gallery.core.ui.generated.resources.download
import com.dot.gallery.core.ui.generated.resources.edit
import com.dot.gallery.core.ui.generated.resources.favorite
import com.dot.gallery.core.ui.generated.resources.hide
import com.dot.gallery.core.ui.generated.resources.media_details
import com.dot.gallery.core.ui.generated.resources.move
import com.dot.gallery.core.ui.generated.resources.move_to_private_folder
import com.dot.gallery.core.ui.generated.resources.rotate
import com.dot.gallery.core.ui.generated.resources.select_all
import com.dot.gallery.core.ui.generated.resources.selection_dialog_close_cd
import com.dot.gallery.core.ui.generated.resources.share
import com.dot.gallery.core.ui.generated.resources.trash
import com.dot.gallery.core.ui.generated.resources.use_as_album_cover

@Serializable
enum class SelectionAction(
    val labelRes: StringResource,
    val descriptionRes: StringResource,
    val zone: ActionZone,
    val minSelection: Int = 1,
    val maxSelection: Int = Int.MAX_VALUE,
    val requiresCondition: ActionCondition = ActionCondition.NONE,
) {
    // ── Top addon actions ──
    CLOSE(
        labelRes = Res.string.selection_dialog_close_cd,
        descriptionRes = Res.string.action_desc_close,
        zone = ActionZone.TOP,
    ),
    SELECT_ALL(
        labelRes = Res.string.select_all,
        descriptionRes = Res.string.action_desc_select_all,
        zone = ActionZone.TOP,
        minSelection = 0,
    ),
    INFO(
        labelRes = Res.string.media_details,
        descriptionRes = Res.string.action_desc_info,
        zone = ActionZone.TOP,
        maxSelection = 1,
    ),

    // ── Middle actions ──
    COLLECTION(
        labelRes = Res.string.add_to_collection,
        descriptionRes = Res.string.action_desc_collection,
        zone = ActionZone.MIDDLE,
    ),

    // ── Bottom bar actions ──
    SHARE(
        labelRes = Res.string.share,
        descriptionRes = Res.string.action_desc_share,
        zone = ActionZone.BOTTOM,
    ),
    FAVORITE(
        labelRes = Res.string.favorite,
        descriptionRes = Res.string.action_desc_favorite,
        zone = ActionZone.BOTTOM,
        requiresCondition = ActionCondition.SUPPORTS_FAVORITES,
    ),
    COPY(
        labelRes = Res.string.copy,
        descriptionRes = Res.string.action_desc_copy,
        zone = ActionZone.BOTTOM,
    ),
    MOVE(
        labelRes = Res.string.move,
        descriptionRes = Res.string.action_desc_move,
        zone = ActionZone.BOTTOM,
    ),
    TRASH(
        labelRes = Res.string.trash,
        descriptionRes = Res.string.action_desc_trash,
        zone = ActionZone.BOTTOM,
    ),
    ADD_TO_VAULT(
        labelRes = Res.string.hide,
        descriptionRes = Res.string.action_desc_hide,
        zone = ActionZone.BOTTOM,
    ),
    MOVE_TO_PRIVATE_FOLDER(
        labelRes = Res.string.move_to_private_folder,
        descriptionRes = Res.string.action_desc_move_to_private_folder,
        zone = ActionZone.BOTTOM,
    ),
    EDIT(
        labelRes = Res.string.edit,
        descriptionRes = Res.string.action_desc_edit,
        zone = ActionZone.BOTTOM,
        maxSelection = 1,
    ),
    ROTATE(
        labelRes = Res.string.rotate,
        descriptionRes = Res.string.action_desc_rotate,
        zone = ActionZone.BOTTOM,
    ),
    DOWNLOAD(
        labelRes = Res.string.download,
        descriptionRes = Res.string.action_desc_download,
        zone = ActionZone.BOTTOM,
    ),
    SET_AS_ALBUM_COVER(
        labelRes = Res.string.use_as_album_cover,
        descriptionRes = Res.string.action_desc_set_as_album_cover,
        zone = ActionZone.BOTTOM,
        maxSelection = 1,
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
            MOVE_TO_PRIVATE_FOLDER -> Icons.Outlined.FolderSpecial
            EDIT -> Icons.Outlined.Edit
            ROTATE -> Icons.AutoMirrored.Outlined.RotateRight
            DOWNLOAD -> Icons.Outlined.Download
            SET_AS_ALBUM_COVER -> Icons.Outlined.Wallpaper
        }
}
