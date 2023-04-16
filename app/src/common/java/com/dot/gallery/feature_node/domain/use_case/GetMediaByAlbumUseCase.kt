/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.use_case

import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetMediaByAlbumUseCase(
    private val repository: MediaRepository
) {
    operator fun invoke(
        albumId: Long,
        mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)
    ): Flow<List<Media>> {
        return repository.getMediaByAlbumId(albumId).map {
            mediaOrder.sortMedia(it)
        }
    }

}

