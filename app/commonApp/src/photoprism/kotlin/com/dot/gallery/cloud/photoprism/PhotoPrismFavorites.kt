/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.photoprism

import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.data.dao.CloudMediaDao
import com.dot.gallery.data.entity.CloudMediaEntity

/**
 * Persists PhotoPrism favorite flags in Room.
 *
 * Album and timeline sync use REPLACE inserts that would otherwise clobber a
 * favorite the user just set. Callers that already hold a successful API result
 * write through [persist]. A favorites listing uses [sync], which insert-ignores
 * missing rows and then updates only the favorite column.
 */
class PhotoPrismFavorites(
    private val cloudMediaDao: CloudMediaDao
) {

    suspend fun persist(remoteId: String, serverConfigId: Long, favorite: Boolean) {
        cloudMediaDao.updateFavorite(
            remoteId = remoteId,
            providerType = ProviderType.PHOTOPRISM,
            serverConfigId = serverConfigId,
            favorite = favorite
        )
    }

    suspend fun sync(fromApi: List<CloudMediaEntity>): List<CloudMediaEntity> {
        if (fromApi.isEmpty()) return emptyList()
        cloudMediaDao.insertAllIgnore(fromApi)
        val configId = fromApi.first().serverConfigId
        for (item in fromApi) {
            cloudMediaDao.updateFavorite(
                remoteId = item.remoteId,
                providerType = ProviderType.PHOTOPRISM,
                serverConfigId = configId,
                favorite = item.favorite
            )
        }
        val existing = fromApi.map { it.remoteId }.chunked(REMOTE_ID_CHUNK).flatMap { chunk ->
            cloudMediaDao.getByRemoteIds(
                remoteIds = chunk,
                providerType = ProviderType.PHOTOPRISM,
                serverConfigId = configId
            )
        }.associateBy { it.remoteId }
        return fromApi.map { api -> existing[api.remoteId] ?: api }
    }

    private companion object {
        const val REMOTE_ID_CHUNK = 500
    }
}
