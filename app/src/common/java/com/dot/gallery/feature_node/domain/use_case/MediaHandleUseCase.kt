/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.use_case

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.dot.gallery.core.Settings
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import javax.inject.Inject

class MediaHandleUseCase @Inject constructor(
    private val repository: MediaRepository,
    private val settings: Settings
) {

    suspend fun toggleFavorite(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>,
        favorite: Boolean = true
    ) = repository.toggleFavorite(result, mediaList, favorite)

    suspend fun trashMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>,
        trash: Boolean = true
    ) {
        /**
         * Trash media only if user enabled the Trash Can
         * Or if user wants to remove existing items from the trash
         * */
        if (settings.trashCanEnabled || !trash)
            repository.trashMedia(result, mediaList, trash)
        else {
            repository.deleteMedia(result, mediaList)
        }
    }

    suspend fun deleteMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>
    ) = repository.deleteMedia(result, mediaList)
}