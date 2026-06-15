/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.data.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Query("SELECT * FROM people ORDER BY faceCount DESC")
    fun getAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE providerType = :type ORDER BY faceCount DESC")
    fun getByProvider(type: ProviderType): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun getById(id: String): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(people: List<PersonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(person: PersonEntity)

    @Query("DELETE FROM people WHERE providerType = :type")
    suspend fun deleteByProvider(type: ProviderType)

    @Query("DELETE FROM people")
    suspend fun deleteAll()
}
