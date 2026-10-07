/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.repository

import com.dot.gallery.domain.uri.Uri

sealed interface CaptureDateEditResult {
    data object Updated : CaptureDateEditResult
    data class CopyCreated(val uri: Uri, val canTrashOriginal: Boolean) : CaptureDateEditResult
    data class NeedsCopy(val capability: CaptureDateEditCapability) : CaptureDateEditResult
    data class Failed(val reason: String) : CaptureDateEditResult
}