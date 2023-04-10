/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.use_case

data class MediaUseCases(
    val addMediaUseCase: AddMediaUseCase,
    val getAlbumsUseCase: GetAlbumsUseCase,
    val getMediaUseCase: GetMediaUseCase,
    val getMediaByAlbumUseCase: GetMediaByAlbumUseCase,
    val getMediaFavoriteUseCase: GetMediaFavoriteUseCase,
    val getMediaTrashedUseCase: GetMediaTrashedUseCase,
    val getMediaByUriUseCase: GetMediaByUriUseCase,
    val mediaHandleUseCase: MediaHandleUseCase
)
