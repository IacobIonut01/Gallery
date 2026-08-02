/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.domain.model

import java.security.MessageDigest

object AlbumMergeResolver {

    fun mergeSubfolders(albums: List<Album>, configs: List<MergedSubfolderAlbum>): List<Album> {
        if (configs.isEmpty()) return albums
        val albumById = albums.associateBy { it.id }
        val configuredParents = configs.mapNotNull { config ->
            albums.firstOrNull {
                config.folderKey != null &&
                    it.volume == config.volume &&
                    it.relativePath.trim('/') == config.relativePath.trim('/')
            } ?: albumById[config.id]?.takeIf { config.folderKey == null }
                ?: config.toVirtualParent(albums)
        }.sortedWith(compareBy<Album>({ it.relativePath.pathDepth() }, { it.relativePath }, { it.id }))
        if (configuredParents.isEmpty()) return albums

        val activeParents = configuredParents.filter { candidate ->
            configuredParents.none { parent ->
                parent.id != candidate.id && parent.contains(candidate)
            }
        }
        if (activeParents.isEmpty()) return albums

        val ownerByAlbumId = HashMap<Long, Album>()
        for (album in albums) {
            activeParents.firstOrNull { it.id == album.id || it.contains(album) }
                ?.let { ownerByAlbumId[album.id] = it }
        }
        val relatedByParentId = ownerByAlbumId.entries.groupBy(
            keySelector = { it.value.id },
            valueTransform = { entry -> albumById.getValue(entry.key) }
        )
        val mergedByParentId = activeParents.mapNotNull { parent ->
            val related = relatedByParentId[parent.id].orEmpty()
            if (related.isEmpty() || related.size == 1 && related.first().id == parent.id) {
                return@mapNotNull null
            }
            val aggregateInputs = if (related.any { it.id == parent.id }) related else listOf(parent) + related
            parent.id to parent.aggregate(
                related = aggregateInputs,
                reason = AlbumMergeReason.SUBFOLDERS
            )
        }.toMap()
        if (mergedByParentId.isEmpty()) return albums

        return buildList(albums.size + mergedByParentId.size) {
            for (album in albums) {
                val owner = ownerByAlbumId[album.id]
                when {
                    owner == null -> add(album)
                    owner.id == album.id -> add(mergedByParentId[owner.id] ?: album)
                }
            }
            for (parent in activeParents) {
                if (parent.id !in albumById) mergedByParentId[parent.id]?.let(::add)
            }
        }
    }

    fun mergeByName(albums: List<Album>): List<Album> = albums
        .groupBy { if (it.mergesSubfolders) "${it.label}\u0000${it.id}" else it.label }
        .values
        .flatMap { sameNameAlbums ->
            if (sameNameAlbums.size <= 1) sameNameAlbums
            else {
                val primary = sameNameAlbums.maxBy { it.timestamp }
                listOf(primary.aggregate(sameNameAlbums, AlbumMergeReason.SAME_NAME))
            }
        }

    fun resolveSourceAlbumIds(requestedAlbumId: Long, albums: List<Album>): List<Long> {
        val album = albums.firstOrNull { it.id == requestedAlbumId }
        return album?.sourceAlbumIds?.distinct()?.sorted() ?: listOf(requestedAlbumId)
    }

    fun parentFolder(album: Album): Pair<String, String>? {
        val relativePath = album.relativePath.trim('/')
        val parentPath = relativePath.substringBeforeLast('/', missingDelimiterValue = "")
        if (parentPath.isBlank()) return null
        return album.volume to "$parentPath/"
    }

    fun isVirtualAlbumId(id: Long): Boolean = id >= VIRTUAL_ALBUM_ID_BASE

    fun virtualAlbumId(volume: String, relativePath: String): Long {
        val key = MergedSubfolderAlbum.folderKey(volume, relativePath)
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        var hash = 0L
        for (index in 0 until 8) {
            hash = (hash shl 8) or (digest[index].toLong() and 0xFFL)
        }
        val positive = hash and Long.MAX_VALUE
        return VIRTUAL_ALBUM_ID_BASE + positive % (Long.MAX_VALUE - VIRTUAL_ALBUM_ID_BASE)
    }

    private fun MergedSubfolderAlbum.toVirtualParent(albums: List<Album>): Album? {
        if (volume.isBlank() || relativePath.isBlank()) return null
        val descendants = albums.filter {
            it.volume == volume &&
                it.relativePath.normalizedDirectoryPath().startsWith(relativePath.normalizedDirectoryPath())
        }
        val thumbnail = descendants.maxByOrNull { it.timestamp } ?: return null
        return Album(
            id = id,
            label = relativePath.trim('/').substringAfterLast('/'),
            uri = thumbnail.uri,
            pathToThumbnail = thumbnail.pathToThumbnail,
            relativePath = relativePath,
            timestamp = thumbnail.timestamp,
            storageVolume = volume
        )
    }

    private fun Album.aggregate(related: List<Album>, reason: AlbumMergeReason): Album {
        val sourceIds = related.flatMap { it.sourceAlbumIds }.filter { it < VIRTUAL_ALBUM_ID_BASE }.distinct()
        val reasons = (related.flatMap { it.mergeReasons } + reason).distinct()
        return copy(
            count = related.sumOf { it.count },
            size = related.sumOf { it.size },
            timestamp = related.maxOf { it.timestamp },
            isPinned = related.any { it.isPinned },
            isLocked = related.any { it.isLocked },
            mergedAlbumIds = sourceIds,
            mergeReasons = reasons
        )
    }

    private fun Album.contains(other: Album): Boolean {
        if (volume != other.volume) return false
        val parentPath = relativePath.normalizedDirectoryPath()
        return other.relativePath.normalizedDirectoryPath().startsWith(parentPath)
    }

    private fun String.normalizedDirectoryPath(): String = trim('/').let {
        if (it.isEmpty()) "" else "$it/"
    }

    private fun String.pathDepth(): Int = trim('/').count { it == '/' }

    private const val VIRTUAL_ALBUM_ID_BASE = 1L shl 32
}
