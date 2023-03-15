package com.dot.gallery.feature_node.domain.use_case

import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository

class DeleteMediaUseCase(
    private val repository: MediaRepository
) {

    /**
     * Deletes media from the database
     * @param media: Media entity
     */
    suspend operator fun invoke(media: Media) {
        repository.deleteMedia(media)
    }

    /**
     * Deletes media based on their id
     * @param id: refers to the Media's unique id
     */
    suspend operator fun invoke(id: Long) {
        repository.deleteMedia(id)
    }
}