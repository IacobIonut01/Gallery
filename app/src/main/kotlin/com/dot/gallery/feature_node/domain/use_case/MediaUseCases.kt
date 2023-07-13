/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.use_case

import android.content.Context
import com.dot.gallery.feature_node.domain.repository.MediaRepository

data class MediaUseCases(
    private val context: Context,
    private val repository: MediaRepository
) {
    val getAlbumsUseCase = GetAlbumsUseCase(repository)
    val getAlbumsWithTypeUseCase = GetAlbumsWithTypeUseCase(repository)
    val getMediaUseCase = GetMediaUseCase(repository)
    val getMediaByAlbumUseCase = GetMediaByAlbumUseCase(repository)
    val getMediaByAlbumWithTypeUseCase = GetMediaByAlbumWithTypeUseCase(repository)
    val getMediaFavoriteUseCase = GetMediaFavoriteUseCase(repository)
    val getMediaTrashedUseCase = GetMediaTrashedUseCase(repository)
    val getMediaByTypeUseCase = GetMediaByTypeUseCase(repository)
    val getMediaByUriUseCase = GetMediaByUriUseCase(repository)
    val getMediaListByUrisUseCase = GetMediaListByUrisUseCase(repository)
    val mediaHandleUseCase = MediaHandleUseCase(repository, context)
    val insertPinnedAlbumUseCase = InsertPinnedAlbumUseCase(repository)
    val deletePinnedAlbumUseCase = DeletePinnedAlbumUseCase(repository)
}