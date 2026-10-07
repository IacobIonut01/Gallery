package com.dot.gallery.feature.viewer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.dot.gallery.core.presentation.components.ConfirmationSheet
import com.dot.gallery.core.ui.util.rememberAppBottomSheetState
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.Vault
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.restore
import com.dot.gallery.feature.viewer.generated.resources.vault_confirm_restore_summary
import com.dot.gallery.feature.viewer.generated.resources.vault_confirm_restore_title


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
        title = stringResource(Res.string.restore)
    ) {
        scope.launch { confirmState.show() }
    }
    ConfirmationSheet(
        state = confirmState,
        title = stringResource(Res.string.vault_confirm_restore_title),
        summary = stringResource(Res.string.vault_confirm_restore_summary),
        onConfirm = {
            scope.launch {
                restoreMedia(currentVault, media) {}
            }
        }
    )
}
