package com.dot.gallery.feature_node.domain.use_case

import com.dot.gallery.feature_node.domain.repository.MediaRepository

class GetMediaByAlbumUseCase(
    private val repository: MediaRepository
) {
/*
    operator fun invoke(
        albumId: Long,
        mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)
    ): Flow<List<Media>> {
        return repository.getMediaByAlbumId(albumId).map { media ->
            when (mediaOrder.orderType) {
                OrderType.Ascending -> {
                    when (mediaOrder) {
                        is MediaOrder.Date -> media.sortedBy { it.timestamp }
                        is MediaOrder.Label -> media.sortedBy { it.label.lowercase() }
                    }
                }
                OrderType.Descending -> {
                    when (mediaOrder) {
                        is MediaOrder.Date -> media.sortedByDescending { it.timestamp }
                        is MediaOrder.Label -> media.sortedByDescending { it.label.lowercase() }
                    }
                }
            }
        }
    }*/

}

