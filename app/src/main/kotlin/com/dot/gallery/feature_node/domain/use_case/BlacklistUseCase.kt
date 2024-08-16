package com.dot.gallery.feature_node.domain.use_case

import com.dot.gallery.feature_node.domain.model.IgnoredAlbum
import com.dot.gallery.feature_node.domain.repository.MediaRepository

class BlacklistUseCase(
    private val repository: MediaRepository
) {

    suspend fun addToBlacklist(ignoredAlbum: IgnoredAlbum) =
        repository.addBlacklistedAlbum(ignoredAlbum)

    suspend fun removeFromBlacklist(ignoredAlbum: IgnoredAlbum) =
        repository.removeBlacklistedAlbum(ignoredAlbum)

    val blacklistedAlbums by lazy { repository.getBlacklistedAlbums() }

}