/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.exif

import android.media.MediaScannerConnection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.dot.gallery.core.LocalMediaHandler
import com.dot.gallery.core.ui.util.AppBottomSheetState
import com.dot.gallery.core.util.ext.renameMedia
import com.dot.gallery.data.entity.MediaMetadata
import com.dot.gallery.domain.model.Media
import com.dot.gallery.feature.exif.ui.MetadataEditSheetContent
import com.dot.gallery.core.ui.util.launchWriteRequest
import com.dot.gallery.core.ui.util.rememberActivityResult
import com.dot.gallery.app.presentation.util.toastError
import com.dot.gallery.core.ui.util.writeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.dot.gallery.core.logging.printDebug

@Composable
fun <T : Media> MetadataEditSheet(
    state: AppBottomSheetState,
    media: T,
    metadata: MediaMetadata?
) {
    val handler = LocalMediaHandler.current
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current
    val cr = remember(context) { context.contentResolver }
    val errorToast = toastError()
    var pendingLabel by remember { mutableStateOf(media.label) }
    var pendingDescription by remember { mutableStateOf<String?>(null) }

    val doUpdate: () -> Unit = {
        scope.launch {
            var done: Boolean
            printDebug("Updating media image description to $pendingDescription")
            done = handler.updateMediaDescription(media, pendingDescription ?: "")
            printDebug("Updated : $done")
            if (pendingLabel != media.label && pendingLabel.isNotBlank()) {
                done = handler.renameMedia(media, pendingLabel)
            }
            if (done) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(media.path),
                    arrayOf(media.mimeType),
                    null
                )
                state.hide()
            } else {
                errorToast.show()
            }
        }
    }
    val request = rememberActivityResult { doUpdate() }

    MetadataEditSheetContent(
        state = state,
        media = media,
        metadata = metadata,
        onSave = { newLabel, imageDescription ->
            pendingLabel = newLabel
            pendingDescription = imageDescription
            scope.launch(Dispatchers.Main) {
                request.launchWriteRequest(
                    media.writeRequest(cr),
                    doUpdate
                )
            }
        },
        onCancel = { scope.launch { state.hide() } }
    )
}
