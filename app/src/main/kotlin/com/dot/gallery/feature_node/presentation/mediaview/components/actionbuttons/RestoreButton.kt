package com.dot.gallery.feature_node.presentation.mediaview.components.actionbuttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.presentation.util.rememberAppBottomSheetState
import com.dot.gallery.feature_node.presentation.vault.components.ConfirmationSheet
import kotlinx.coroutines.launch

@Composable
fun <T : Media> RestoreButton(
    media: T,
    currentVault: Vault,
    restoreMedia: (Vault, T, () -> Unit) -> Unit,
    followTheme: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val confirmState = rememberAppBottomSheetState()
    MediaViewButton(
        currentMedia = media,
        imageVector = Icons.Outlined.Restore,
        followTheme = followTheme,
        title = stringResource(R.string.restore)
    ) {
        scope.launch { confirmState.show() }
    }
    ConfirmationSheet(
        state = confirmState,
        title = stringResource(R.string.vault_confirm_restore_title),
        summary = stringResource(R.string.vault_confirm_restore_summary),
        onConfirm = {
            scope.launch {
                restoreMedia(currentVault, media) {}
            }
        }
    )
}