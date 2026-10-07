/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.Vault
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid
import com.dot.gallery.domain.model.EncryptedMedia2

@Dao
interface VaultDao {

    /**
     * Vault Management
     */

    @Query("SELECT * FROM vaults")
    fun getVaults(): Flow<List<Vault>>

    @Upsert
    suspend fun insertVault(vault: Vault)

    @Query("SELECT * FROM vaults WHERE uuid = :uuid LIMIT 1")
    suspend fun getVault(uuid: Uuid): Vault?

    @Delete
    suspend fun deleteVaultInfo(vault: Vault)

    @Transaction
    suspend fun deleteVault(vault: Vault) {
        deleteAllMediaFromVault(vault.uuid)
        deleteVaultInfo(vault)
    }

    @Query("DELETE FROM vaults")
    suspend fun deleteAllVaults()

    /**
     * Vault Media Management
     */

    @Query("SELECT * FROM encrypted_media")
    fun getAllMedia(): Flow<List<EncryptedMedia2>>

    @Query("SELECT * FROM encrypted_media WHERE uuid = :uuid")
    fun getMediaFromVault(uuid: Uuid?): Flow<List<EncryptedMedia2>>

    @Query("SELECT uuid, COUNT(*) as count FROM encrypted_media GROUP BY uuid")
    fun getMediaCountPerVault(): Flow<List<VaultMediaCount>>

    @Query("SELECT EXISTS(SELECT 1 FROM encrypted_media WHERE uuid = :uuid AND id = :id)")
    suspend fun mediaExistsInVault(uuid: Uuid, id: Long): Boolean

    @Upsert
    suspend fun addMediaToVault(media: EncryptedMedia2)

    @Query("DELETE FROM encrypted_media WHERE uuid = :uuid AND id = :id")
    suspend fun deleteMediaFromVault(uuid: Uuid, id: Long): Int

    @Transaction
    suspend fun deleteMediaFromVault(media: EncryptedMedia2): Boolean {
        return deleteMediaFromVault(uuid = media.uuid, id = media.id) > 0
    }

    @Query("DELETE FROM encrypted_media WHERE uuid = :uuid")
    suspend fun deleteAllMediaFromVault(uuid: Uuid)

}
