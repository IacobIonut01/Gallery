package com.dot.gallery.feature_node.domain.use_case

data class MediaUseCases(
    val addMediaUseCase: AddMediaUseCase,
    val deleteMediaUseCase: DeleteMediaUseCase,
    val getMediaUseCase: GetMediaUseCase,
    val getMediaByAlbumUseCase: GetMediaByAlbumUseCase,
    val getMediaByIdUseCase: GetMediaByIdUseCase
)
