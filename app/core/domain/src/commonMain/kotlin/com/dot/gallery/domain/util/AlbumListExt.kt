package com.dot.gallery.domain.util

import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.hiddenInAlbums
import com.dot.gallery.domain.model.IgnoredAlbum
import com.dot.gallery.domain.model.LockedAlbum
import com.dot.gallery.domain.model.PinnedAlbum

fun List<Album>.mapPinned(pinnedAlbums: List<PinnedAlbum>): List<Album> {
    val pinnedIds = pinnedAlbums.mapTo(HashSet(pinnedAlbums.size)) { it.id }
    return map { album -> album.copy(isPinned = album.id in pinnedIds) }
}

fun List<Album>.mapLocked(lockedAlbums: List<LockedAlbum>): List<Album> {
    val lockedIds = lockedAlbums.mapTo(HashSet(lockedAlbums.size)) { it.id }
    return map { album -> album.copy(isLocked = album.id in lockedIds) }
}

fun List<Album>.removeBlacklisted(blacklistedAlbums: List<IgnoredAlbum>): List<Album> =
    toMutableList().apply {
        removeAll { album -> blacklistedAlbums.any { it.matchesAlbum(album) && it.hiddenInAlbums } }
    }
