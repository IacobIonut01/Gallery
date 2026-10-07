/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.exif

import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.dot.gallery.core.LocalMediaHandler
import com.dot.gallery.core.ui.util.AppBottomSheetState
import com.dot.gallery.core.util.SdkCompat
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.getUri
import com.dot.gallery.feature.exif.ui.CaptureDateCopyKind
import com.dot.gallery.feature.exif.ui.CaptureDateEditSheetContent
import com.dot.gallery.data.repository.CaptureDateEditCapability
import com.dot.gallery.data.repository.CaptureDateEditResult
import com.dot.gallery.core.ui.util.launchWriteRequest
import com.dot.gallery.core.ui.util.rememberActivityResult
import com.dot.gallery.core.ui.util.trashRequest
import com.dot.gallery.core.ui.util.writeRequest
import kotlinx.coroutines.launch
import com.dot.gallery.domain.model.definedTimestamp
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.capture_date_failed
import com.dot.gallery.generated.resources.capture_date_saved
import com.dot.gallery.core.util.getString

@Composable
fun CaptureDateEditSheet(
    state: AppBottomSheetState,
    media: Media,
) {
    val context = LocalContext.current
    val handler = LocalMediaHandler.current
    val scope = rememberCoroutineScope()
    var busy by remember(media.id) { mutableStateOf(false) }
    var pendingTimestamp by rememberSaveable(media.id) {
        mutableLongStateOf(media.definedTimestamp * 1000L)
    }
    var copyCapability by remember { mutableStateOf<CaptureDateEditCapability?>(null) }
    var copyCreated by remember { mutableStateOf<CaptureDateEditResult.CopyCreated?>(null) }

    val performUpdate: () -> Unit = {
        busy = true
        scope.launch {
            when (val result = handler.updateMediaCaptureDate(media, pendingTimestamp)) {
                CaptureDateEditResult.Updated -> {
                    Toast.makeText(
context,
    context.getString(Res.string.capture_date_saved), Toast.LENGTH_SHORT).show()
                    state.hide()
                }
                is CaptureDateEditResult.NeedsCopy -> copyCapability = result.capability
                is CaptureDateEditResult.Failed ->
                    Toast.makeText(context, result.reason, Toast.LENGTH_LONG).show()
                is CaptureDateEditResult.CopyCreated -> copyCreated = result
            }
            busy = false
        }
    }
    val writePermission = rememberActivityResult(onResultOk = performUpdate)
    val trashPermission = rememberActivityResult(
        onResultOk = { scope.launch { state.hide() } }
    )

    CaptureDateEditSheetContent(
        state = state,
        media = media,
        busy = busy,
        is24Hour = DateFormat.is24HourFormat(context),
        copyKind = copyCapability?.let {
            if (it == CaptureDateEditCapability.SAFE_COPY) {
                CaptureDateCopyKind.SAFE_COPY
            } else {
                CaptureDateCopyKind.COPY_ONLY
            }
        },
        copyCreated = copyCreated != null,
        canTrashOriginal = copyCreated?.canTrashOriginal == true && SdkCompat.supportsTrash,
        onDismiss = { scope.launch { state.hide() } },
        onSave = { timestamp ->
            pendingTimestamp = timestamp
            scope.launch {
                busy = true
                when (val capability = handler.probeCaptureDateEdit(media)) {
                    CaptureDateEditCapability.DIRECT_WRITE -> {
                        busy = false
                        writePermission.launchWriteRequest(
                            media.writeRequest(context.contentResolver),
                            performUpdate
                        )
                    }
                    CaptureDateEditCapability.SAFE_COPY,
                    CaptureDateEditCapability.COPY_ONLY -> {
                        copyCapability = capability
                        busy = false
                    }
                    CaptureDateEditCapability.UNSUPPORTED -> {
                        busy = false
                        Toast.makeText(
context,
                            context.getString(Res.string.capture_date_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        },
        onCreateCopy = { timestamp ->
            pendingTimestamp = timestamp
            busy = true
            scope.launch {
                when (val result = handler.createDatedCopy(media, timestamp)) {
                    is CaptureDateEditResult.CopyCreated -> {
                        copyCreated = result
                        copyCapability = null
                    }
                    is CaptureDateEditResult.Failed ->
                        Toast.makeText(context, result.reason, Toast.LENGTH_LONG).show()
                    else -> Unit
                }
                busy = false
            }
        },
        onDismissCopy = { copyCapability = null },
        onTrashOriginal = {
            val request = media.getUri().trashRequest(context.contentResolver)
            copyCreated = null
            if (request != null) trashPermission.launch(request)
            else scope.launch { state.hide() }
        },
        onKeepBoth = {
            copyCreated = null
            scope.launch { state.hide() }
        }
    )
}
