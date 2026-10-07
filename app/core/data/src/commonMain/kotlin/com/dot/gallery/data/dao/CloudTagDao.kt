/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.dot.gallery.data.entity.CloudMediaTagEntity
import com.dot.gallery.data.entity.CloudTagEntity
import kotlinx.coroutines.flow.Flow
import com.dot.gallery.domain.uri.query


@Dao
interface CloudTagDao {

    @Query("SELECT * FROM cloud_tag WHERE serverConfigId = :configId")
    fun getTagsForAccount(configId: Long): Flow<List<CloudTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTags(tags: List<CloudTagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLinks(links: List<CloudMediaTagEntity>)

    @Query("DELETE FROM cloud_tag WHERE serverConfigId = :configId")
    suspend fun deleteTagsForAccount(configId: Long)

    @Query("DELETE FROM cloud_media_tag WHERE serverConfigId = :configId")
    suspend fun deleteLinksForAccount(configId: Long)

    @Transaction
    suspend fun replaceForAccount(
        configId: Long,
        tags: List<CloudTagEntity>,
        links: List<CloudMediaTagEntity>
    ) {
        deleteLinksForAccount(configId)
        deleteTagsForAccount(configId)
        upsertTags(tags)
        upsertLinks(links)
    }

    @Transaction
    suspend fun deleteForAccount(configId: Long) {
        deleteLinksForAccount(configId)
        deleteTagsForAccount(configId)
    }

    /**
     * Tag-link rows whose tag name or value contains [query]. SQLite LIKE is
     * case-insensitive for ASCII, matching how metadata search treats tag text.
     */
    @Query(
        "SELECT DISTINCT m.* FROM cloud_media_tag m " +
            "INNER JOIN cloud_tag t " +
            "ON t.serverConfigId = m.serverConfigId AND t.tagId = m.tagId " +
            "WHERE t.name LIKE '%' || :query || '%' OR t.value LIKE '%' || :query || '%'"
    )
    suspend fun findLinksForTagQuery(query: String): List<CloudMediaTagEntity>
}
