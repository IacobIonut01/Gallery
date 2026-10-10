/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.sharing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.injection.injectViewModel
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.cloud_shared_links_link_copied
import org.jetbrains.compose.resources.stringResource


@Composable
fun SharedLinksScreen() {
    val viewModel = injectViewModel<SharedLinksViewModel> {sharedLinksViewModel}
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val linkCopiedText = stringResource(Res.string.cloud_shared_links_link_copied)
    SharedLinksContent(
        state = state,
        actions = SharedLinksContentActions(
            setFilter = viewModel::setFilter,
            copyLink = { link ->
                val url = viewModel.getShareUrl(link)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Shared Link", url))
                Toast.makeText(context, linkCopiedText, Toast.LENGTH_SHORT).show()
            },
            deleteLink = viewModel::deleteLink,
            updateLink = viewModel::updateLink,
        ),
    )
}
