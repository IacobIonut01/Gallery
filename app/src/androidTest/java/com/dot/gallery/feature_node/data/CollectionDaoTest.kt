/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.feature_node.data.data_source.CollectionDao
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import com.dot.gallery.feature_node.domain.model.Collection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionDaoTest {

    private lateinit var db: InternalDatabase
    private lateinit var collectionDao: CollectionDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        collectionDao = db.getCollectionDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun replaceAlbumsInCollection_addsAlbums() = runBlocking {
        val collectionId = collectionDao.insertCollection(Collection(label = "Trips"))

        collectionDao.replaceAlbumsInCollection(collectionId, listOf(10L, 20L))

        assertEquals(
            setOf(10L, 20L),
            collectionDao.getAlbumIdsInCollection(collectionId).first().toSet()
        )
    }

    @Test
    fun replaceAlbumsInCollection_removesDeselectedAlbums() = runBlocking {
        val collectionId = collectionDao.insertCollection(Collection(label = "Trips"))
        collectionDao.replaceAlbumsInCollection(collectionId, listOf(10L, 20L))

        collectionDao.replaceAlbumsInCollection(collectionId, listOf(20L, 30L))

        assertEquals(
            setOf(20L, 30L),
            collectionDao.getAlbumIdsInCollection(collectionId).first().toSet()
        )
    }

    @Test
    fun replaceAlbumsInCollection_clearsAllAlbums() = runBlocking {
        val collectionId = collectionDao.insertCollection(Collection(label = "Trips"))
        collectionDao.replaceAlbumsInCollection(collectionId, listOf(10L, 20L))

        collectionDao.replaceAlbumsInCollection(collectionId, emptyList())

        assertEquals(emptyList<Long>(), collectionDao.getAlbumIdsInCollection(collectionId).first())
    }
}
