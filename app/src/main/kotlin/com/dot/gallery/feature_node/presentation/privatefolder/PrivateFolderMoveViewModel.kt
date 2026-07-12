/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.privatefolder

import androidx.lifecycle.ViewModel
import com.dot.gallery.core.sandbox.PrivateFolderRepository
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.domain.util.isCloud
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Lightweight helper for moving/copying selected media into the private folder
 * from the selection sheet. Unlike [PrivateFolderViewModel] it does not observe
 * the folder scan, so it can be hosted cheaply on the timeline.
 */
@HiltViewModel
class PrivateFolderMoveViewModel @Inject constructor(
    private val repository: PrivateFolderRepository
) : ViewModel() {

    /**
     * Copy every local (non-cloud) item in [media] into the private folder.
     * Returns the subset that was copied successfully so the caller can
     * request deletion of the originals to complete a "move".
     */
    suspend fun <T : Media> copyIntoPrivateFolder(media: List<T>): List<T> =
        withContext(Dispatchers.IO) {
            media.filterNot { it.isCloud }.filter { item ->
                repository.addMedia(
                    sourceUri = item.getUri(),
                    displayName = item.label,
                    mimeType = item.mimeType
                )
            }
        }

    /**
     * Move every item in [media] out of the private folder: each is copied back
     * into the public MediaStore and the private-folder original is deleted via
     * SAF (never a MediaStore delete request, which crashes on tree document
     * URIs — #1015). Returns the count moved successfully.
     */
    suspend fun <T : Media> moveOutOfPrivateFolder(media: List<T>): Int =
        withContext(Dispatchers.IO) {
            media.count { item ->
                repository.moveOut(
                    sourceUri = item.getUri(),
                    displayName = item.label,
                    mimeType = item.mimeType
                )
            }
        }

    /**
     * Permanently delete every item in [media] from the private folder using
     * SAF document deletion. Returns the count deleted successfully.
     */
    suspend fun <T : Media> deleteFromPrivateFolder(media: List<T>): Int =
        withContext(Dispatchers.IO) {
            media.count { item -> repository.deleteByUri(item.getUri()) }
        }
}
