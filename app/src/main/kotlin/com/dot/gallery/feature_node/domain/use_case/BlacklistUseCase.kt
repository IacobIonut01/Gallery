package com.dot.gallery.feature_node.domain.use_case

import com.dot.gallery.feature_node.domain.model.BlacklistedAlbum
import com.dot.gallery.feature_node.domain.repository.MediaRepository

class BlacklistUseCase(
    private val repository: MediaRepository
) {

    suspend fun addToBlacklist(blacklistedAlbum: BlacklistedAlbum) =
        repository.addBlacklistedAlbum(blacklistedAlbum)

    suspend fun removeFromBlacklist(blacklistedAlbum: BlacklistedAlbum) =
        repository.removeBlacklistedAlbum(blacklistedAlbum)

    val blacklistedAlbums by lazy { repository.getBlacklistedAlbums() }

}