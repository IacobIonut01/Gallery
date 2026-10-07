/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud

import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.data.dao.CloudUploadPrefDao
import com.dot.gallery.data.entity.CloudUploadPrefEntity
import com.dot.gallery.data.InternalDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.dot.gallery.domain.model.Album


/**
 * Verifies that per-account backup album selections are correctly isolated and
 * that removing an account cleans up only that account's selections — the core
 * guarantee behind the per-provider backup model.
 */
@RunWith(AndroidJUnit4::class)
class CloudUploadPrefDaoTest {

    private lateinit var db: InternalDatabase
    private lateinit var dao: CloudUploadPrefDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.getCloudUploadPrefDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun pref(
        configId: Long,
        albumId: Long,
        provider: ProviderType,
        enabled: Boolean = true,
        deleteLocal: Boolean = false
    ) = CloudUploadPrefEntity(
        serverConfigId = configId,
        albumId = albumId,
        providerType = provider,
        albumLabel = "Album $albumId",
        uploadEnabled = enabled,
        deleteLocalAfterUpload = deleteLocal
    )

    @Test
    fun sameAlbumCanBeSelectedForDifferentAccounts() = runBlocking {
        // Album 100 is backed up by two different accounts; the composite primary
        // key (serverConfigId, albumId) must keep both rows distinct.
        dao.upsert(pref(configId = 1, albumId = 100, provider = ProviderType.IMMICH))
        dao.upsert(pref(configId = 2, albumId = 100, provider = ProviderType.OWNCLOUD))

        val rowsForAlbum100 = dao.getAll().first().filter { it.albumId == 100L }
        assertEquals(2, rowsForAlbum100.size)
        assertEquals(
            setOf(1L, 2L),
            rowsForAlbum100.map { it.serverConfigId }.toSet()
        )
    }

    @Test
    fun enabledSelectionsAreScopedPerAccount() = runBlocking {
        dao.upsert(pref(configId = 1, albumId = 100, provider = ProviderType.IMMICH, enabled = true))
        dao.upsert(pref(configId = 1, albumId = 200, provider = ProviderType.IMMICH, enabled = false))
        dao.upsert(pref(configId = 2, albumId = 300, provider = ProviderType.OWNCLOUD, enabled = true))

        val account1 = dao.getEnabledByConfigList(1)
        assertEquals(1, account1.size)
        assertEquals(100L, account1.single().albumId)

        val account2 = dao.getEnabledByConfigList(2)
        assertEquals(1, account2.size)
        assertEquals(300L, account2.single().albumId)
    }

    @Test
    fun deleteByConfigRemovesOnlyThatAccountsSelections() = runBlocking {
        dao.upsert(pref(configId = 1, albumId = 100, provider = ProviderType.IMMICH))
        dao.upsert(pref(configId = 1, albumId = 200, provider = ProviderType.IMMICH))
        dao.upsert(pref(configId = 2, albumId = 100, provider = ProviderType.OWNCLOUD))

        dao.deleteByConfig(1)

        assertTrue(dao.getByConfig(1).first().isEmpty())
        assertEquals(1, dao.getByConfig(2).first().size)
    }

    @Test
    fun deleteSingleAlbumDoesNotAffectOtherAccounts() = runBlocking {
        dao.upsert(pref(configId = 1, albumId = 100, provider = ProviderType.IMMICH))
        dao.upsert(pref(configId = 2, albumId = 100, provider = ProviderType.OWNCLOUD))

        dao.delete(configId = 1, albumId = 100)

        assertTrue(dao.getByConfig(1).first().isEmpty())
        assertEquals(1, dao.getByConfig(2).first().size)
    }
}
