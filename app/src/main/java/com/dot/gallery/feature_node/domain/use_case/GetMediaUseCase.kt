package com.dot.gallery.feature_node.domain.use_case

import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {

    operator fun invoke(): Flow<Resource<List<Media>>> = repository.getMedia()

}