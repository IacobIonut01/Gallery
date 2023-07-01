/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.use_case

import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetMediaByAlbumWithTypeUseCase(
    private val repository: MediaRepository
) {
    operator fun invoke(
        albumId: Long,
        type: AllowedMedia,
        mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)
    ): Flow<Resource<List<Media>>> {
        return repository.getMediaByAlbumIdWithType(albumId, type).map {
            it.apply {
                data = data?.let { it1 -> mediaOrder.sortMedia(it1) }
            }
        }
    }

}

