/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.data.dao.CloudAlbumSyncDao
import com.dot.gallery.cloud.data.entity.CloudAlbumSyncEntity
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloudAlbumSyncDaoTest {

    private lateinit var database: InternalDatabase
    private lateinit var dao: CloudAlbumSyncDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.getCloudAlbumSyncDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sameProviderAccountsKeepIndependentAlbumPreferences() = runBlocking {
        dao.upsert(album(configId = 11L, enabled = false))
        dao.upsert(album(configId = 22L, enabled = true))

        assertEquals(2, dao.getAll().first().size)
        assertFalse(dao.isSyncEnabled("shared-album", ProviderType.IMMICH, 11L)!!)
        assertTrue(dao.isSyncEnabled("shared-album", ProviderType.IMMICH, 22L)!!)

        dao.setSyncEnabled("shared-album", ProviderType.IMMICH, 11L, true)

        assertTrue(dao.isSyncEnabled("shared-album", ProviderType.IMMICH, 11L)!!)
        assertTrue(dao.isSyncEnabled("shared-album", ProviderType.IMMICH, 22L)!!)
        assertEquals(listOf(11L), dao.getByServer(11L).first().map { it.serverConfigId })
    }

    private fun album(configId: Long, enabled: Boolean) = CloudAlbumSyncEntity(
        albumRemoteId = "shared-album",
        providerType = ProviderType.IMMICH,
        serverConfigId = configId,
        albumName = "Shared album",
        syncEnabled = enabled
    )
}
