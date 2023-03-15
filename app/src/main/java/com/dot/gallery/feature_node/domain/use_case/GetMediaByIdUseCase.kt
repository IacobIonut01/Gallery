package com.dot.gallery.feature_node.domain.use_case

import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository

class GetMediaByIdUseCase(
    private val repository: MediaRepository
) {

    suspend operator fun invoke(id: Long): Media? {
        return repository.getMediaById(id)
    }

}

