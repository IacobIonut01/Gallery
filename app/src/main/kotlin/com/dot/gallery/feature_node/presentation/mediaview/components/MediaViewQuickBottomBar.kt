package com.dot.gallery.feature_node.presentation.mediaview.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R
import com.dot.gallery.cloud.core.CloudRuntimeSettings
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.LocalMediaHandler
import com.dot.gallery.core.Settings.Misc.rememberAllowBlur
import com.dot.gallery.core.Settings.Misc.rememberShowFavoriteButton
import com.dot.gallery.core.util.SdkCompat
import com.dot.gallery.core.setFollowTheme
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.util.canMakeActions
import com.dot.gallery.feature_node.domain.util.isCloud
import com.dot.gallery.feature_node.domain.util.isEncrypted
import com.dot.gallery.feature_node.domain.util.isRaw
import com.dot.gallery.feature_node.domain.util.isTrashed
import com.dot.gallery.feature_node.domain.util.isVideo
import com.dot.gallery.feature_node.domain.util.readUriOnly
import com.dot.gallery.feature_node.presentation.mediaview.components.actionbuttons.CopyToClipboardButton
import com.dot.gallery.feature_node.presentation.mediaview.components.actionbuttons.DownloadButton
import com.dot.gallery.feature_node.presentation.mediaview.components.actionbuttons.EditButton
import com.dot.gallery.feature_node.presentation.mediaview.components.actionbuttons.FavoriteButton
import com.dot.gallery.feature_node.presentation.mediaview.components.actionbuttons.MediaViewButton
import com.dot.gallery.feature_node.presentation.mediaview.components.actionbuttons.OpenAsButton
import com.dot.gallery.feature_node.presentation.mediaview.components.actionbuttons.PrivateFolderDeleteButton
import com.dot.gallery.feature_node.presentation.mediaview.components.actionbuttons.RestoreButton
import com.dot.gallery.feature_node.presentation.mediaview.components.actionbuttons.ShareButton
import com.dot.gallery.feature_node.presentation.mediaview.components.actionbuttons.TrashButton
import com.dot.gallery.feature_node.presentation.mediaview.rememberedDerivedState
import com.dot.gallery.feature_node.presentation.privatefolder.PrivateFolderViewModel
import com.dot.gallery.feature_node.presentation.util.rememberActivityResult
import kotlinx.coroutines.launch

@Composable
fun <T : Media> MediaViewQuickBottomBar(
    currentMedia: T?,
    showDeleteButton: Boolean,
    enabled: Boolean,
    deleteMedia: ((Vault, T, () -> Unit) -> Unit)?,
    restoreMedia: ((Vault, T, () -> Unit) -> Unit)?,
    currentVault: Vault?,
    isImageDark: Boolean = false,
    autoContrast: Boolean = false,
    onTrashConfirmed: () -> Unit = {}
) {
    val handler = LocalMediaHandler.current
    val allowBlur by rememberAllowBlur()
    val isVideo by rememberedDerivedState(currentMedia) {
        currentMedia?.isVideo ?: false
    }
    val isDarkTheme = com.dot.gallery.ui.theme.isDarkTheme()
    val followTheme = remember(allowBlur, isVideo, isDarkTheme, autoContrast, isImageDark) {
        if (autoContrast) !isImageDark
        else !allowBlur && !isVideo
    }
    val contentColor by animateColorAsState(
        targetValue = when {
            autoContrast -> if (isImageDark) Color.White else Color.Black
            followTheme -> MaterialTheme.colorScheme.onSurface
            else -> Color.White
        },
        label = "BottomBarContentColor"
    )
    val eventHandler = LocalEventHandler.current
    LaunchedEffect(followTheme) {
        eventHandler.setFollowTheme(followTheme)
    }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
    if (currentMedia != null) {
        if (currentMedia.isTrashed) {
            val scope = rememberCoroutineScope()
            val result = rememberActivityResult(onResultOk = onTrashConfirmed)
            // Restore Component
            MediaViewButton(
                currentMedia = currentMedia,
                imageVector = Icons.Outlined.RestoreFromTrash,
                title = stringResource(id = R.string.trash_restore),
                followTheme = followTheme,
                enabled = enabled
            ) {
                scope.launch {
                    handler.trashMedia(result = result, arrayListOf(it), trash = false)
                }
            }
            // Delete Component
            MediaViewButton(
                currentMedia = currentMedia,
                imageVector = Icons.Outlined.DeleteOutline,
                title = stringResource(id = R.string.trash_delete),
                enabled = enabled
            ) {
                scope.launch {
                    handler.deleteMedia(result = result, arrayListOf(it))
                }
            }
        } else {
            // Read-only mode (cloud Advanced setting): hide all write/share actions for cloud
            // media so it can be browsed but never modified, shared, edited or deleted.
            val readOnly = currentMedia.isCloud && CloudRuntimeSettings.readOnlyMode
            // Share Component
            if (!readOnly) {
                ShareButton(
                    media = currentMedia,
                    enabled = enabled,
                    followTheme = followTheme,
                    currentVault = currentVault
                )
                // Copy to Clipboard
                CopyToClipboardButton(
                    media = currentMedia,
                    enabled = enabled,
                    followTheme = followTheme,
                    currentVault = currentVault
                )
            }
            // Favorite Component
            val showFavoriteButton by rememberShowFavoriteButton()
            if (!readOnly && showFavoriteButton && (currentMedia.canMakeActions && SdkCompat.supportsFavorites || currentMedia.isCloud)) {
                FavoriteButton(
                    media = currentMedia,
                    enabled = enabled,
                    followTheme = followTheme
                )
            }
            if (currentMedia.readUriOnly) {
                OpenAsButton(
                    media = currentMedia,
                    enabled = enabled,
                    followTheme = followTheme
                )
            }
            // Restore
            if (currentMedia.isEncrypted && restoreMedia != null && currentVault != null) {
                RestoreButton(
                    media = currentMedia,
                    currentVault = currentVault,
                    restoreMedia = restoreMedia,
                    followTheme = followTheme
                )
            }
            // Download (cloud only)
            if (currentMedia.isCloud) {
                DownloadButton(
                    media = currentMedia,
                    enabled = enabled,
                    followTheme = followTheme
                )
            }
            // Develop RAW (native LibRaw): only for RAW media when the native lib is available.
            if (currentMedia.isRaw && !currentMedia.isEncrypted && !readOnly &&
                com.dot.gallery.core.decoder.NativeRawDecoder.isAvailable
            ) {
                com.dot.gallery.feature_node.presentation.mediaview.components.rawdevelop.RawDevelopButton(
                    media = currentMedia,
                    enabled = enabled,
                    followTheme = followTheme
                )
            }
            // Edit
            if (!currentMedia.isEncrypted && !readOnly) {
                EditButton(
                    media = currentMedia,
                    enabled = enabled,
                    followTheme = followTheme
                )
            }
            // Trash Component
            if (showDeleteButton && !readOnly) {
                // Private-folder items are SAF documents; deleting them via a
                // MediaStore request crashes (#1015). Route them through a
                // SAF-only delete instead of the regular TrashButton.
                if (currentMedia.albumID == PrivateFolderViewModel.PRIVATE_FOLDER_ALBUM_ID) {
                    PrivateFolderDeleteButton(
                        media = currentMedia,
                        enabled = enabled,
                        followTheme = followTheme,
                        onDeleted = onTrashConfirmed
                    )
                } else {
                    TrashButton(
                        media = currentMedia,
                        enabled = enabled,
                        deleteMedia = deleteMedia,
                        currentVault = currentVault,
                        followTheme = followTheme,
                        onTrashConfirmed = onTrashConfirmed
                    )
                }
            }
        }
    }
    }
}