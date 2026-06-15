package com.dot.gallery.feature_node.presentation.mediaview.components.actionbuttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R
import com.dot.gallery.feature_node.data.data_source.KeychainHolder
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.util.isEncrypted
import com.dot.gallery.feature_node.presentation.util.copyEncryptedMediaToClipboard
import com.dot.gallery.feature_node.presentation.util.copyMediaToClipboard
import kotlinx.coroutines.launch

@Composable
fun <T : Media> CopyToClipboardButton(
    media: T,
    enabled: Boolean,
    followTheme: Boolean = false,
    currentVault: Vault? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    MediaViewButton(
        currentMedia = media,
        imageVector = Icons.Outlined.ContentCopy,
        followTheme = followTheme,
        title = stringResource(R.string.copy_to_clipboard),
        enabled = enabled
    ) {
        scope.launch {
            if (it.isEncrypted && currentVault != null) {
                val keychainHolder = KeychainHolder(context)
                context.copyEncryptedMediaToClipboard(it, keychainHolder)
            } else {
                context.copyMediaToClipboard(it)
            }
        }
    }
}
