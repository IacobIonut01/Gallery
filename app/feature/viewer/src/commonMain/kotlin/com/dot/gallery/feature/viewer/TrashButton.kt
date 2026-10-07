package com.dot.gallery.feature.viewer
import com.dot.gallery.domain.trash.resolveTrashDialogAction
import com.dot.gallery.domain.trash.TrashDialogAction

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.dot.gallery.core.ui.util.AppBottomSheetState
import com.dot.gallery.core.ui.util.rememberAppBottomSheetState
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.isCloud
import com.dot.gallery.domain.util.isEncrypted
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.action_delete_permanently
import com.dot.gallery.feature.viewer.generated.resources.trash


/**
 * Trash/delete button with its long-press "delete permanently" escape hatch.
 *
 * The trash/delete confirmation dialog (with cloud-backup scope picking) and the
 * actual MediaStore mutation are app-side — supplied via [dialog], which receives
 * the sheet state and the resolved [TrashDialogAction].
 */
@Composable
fun <T : Media> TrashButton(
    media: T,
    enabled: Boolean,
    followTheme: Boolean = false,
    trashEnabled: Boolean,
    trashSupported: Boolean,
    cloudSupportsTrash: Boolean = false,
    dialog: @Composable (state: AppBottomSheetState, action: TrashDialogAction) -> Unit
) {
    var shouldMoveToTrash by rememberSaveable { mutableStateOf(true) }
    val state = rememberAppBottomSheetState()
    val scope = rememberCoroutineScope()
    val effectiveAction = resolveTrashDialogAction(
        trashRequested = shouldMoveToTrash && !media.isEncrypted,
        trashEnabled = if (media.isCloud) cloudSupportsTrash else trashEnabled,
        trashSupported = if (media.isCloud) cloudSupportsTrash else trashSupported
    )
    MediaViewButton(
        currentMedia = media,
        imageVector = Icons.Outlined.DeleteOutline,
        followTheme = followTheme,
        title = stringResource(
            if (effectiveAction == TrashDialogAction.TRASH) {
                Res.string.trash
            } else {
                Res.string.action_delete_permanently
            }
        ),
        onItemLongClick = {
            shouldMoveToTrash = false
            scope.launch {
                state.show()
            }
        },
        onItemClick = {
            shouldMoveToTrash = true
            scope.launch {
                state.show()
            }
        },
        enabled = enabled
    )

    dialog(state, effectiveAction)
}
