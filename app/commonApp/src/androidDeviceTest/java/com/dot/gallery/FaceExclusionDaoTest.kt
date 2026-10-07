/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery

import androidx.room3.Room
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.data.dao.DetectedFaceDao
import com.dot.gallery.data.dao.PersonDao
import com.dot.gallery.data.entity.DetectedFaceEntity
import com.dot.gallery.data.entity.FaceClusterEntity
import com.dot.gallery.data.entity.FaceExclusionEntity
import com.dot.gallery.data.entity.PersonEntity
import com.dot.gallery.data.InternalDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class FaceExclusionDaoTest {
    private lateinit var db: InternalDatabase
    private lateinit var faceDao: DetectedFaceDao
    private lateinit var personDao: PersonDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java)
            .setDriver(BundledSQLiteDriver())
            .allowMainThreadQueries()
            .build()
        faceDao = db.getDetectedFaceDao()
        personDao = db.getPersonDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insertPerson(id: String) =
        personDao.insert(PersonEntity(id, "", ProviderType.LOCAL_PEOPLE))

    private suspend fun insertMediaRow(id: Long) {
        db.useWriterConnection { connection ->
            connection.usePrepared(
                """
                INSERT INTO media (
                    id, label, uri, path, relativePath, albumID, albumLabel,
                    timestamp, fullDate, mimeType, favorite, trashed, size
                ) VALUES (?, 'img.jpg', 'content://media/external/$id', '/x/img.jpg',
                          'x/', 1, 'Album', 100, 'date', 'image/jpeg', 0, 0, 1)
                """.trimIndent()
            ) { statement ->
                statement.bindLong(1, id)
                statement.step()
            }
        }
    }

    @Test
    fun unassignKeepsFaceRowAndRecordsExclusion() = runBlocking {
        insertPerson("personA")
        insertPerson("personB")
        faceDao.insert(DetectedFaceEntity(mediaId = 7L, personId = "personA", embedding = byteArrayOf(1)))
        faceDao.insert(DetectedFaceEntity(mediaId = 8L, personId = "personB", embedding = byteArrayOf(2)))

        faceDao.insertExclusions(listOf(FaceExclusionEntity(7L, "personA", 10L)))
        assertEquals(1, faceDao.unassignPersonMedia("personA", listOf(7L)))

        val unassigned = faceDao.getByMedia(7L).single()
        assertNull(unassigned.personId)
        assertEquals(emptyList<Long>(), faceDao.getMediaIdsForPerson("personA"))
        // The other person in another photo is untouched.
        assertEquals("personB", faceDao.getByMedia(8L).single().personId)
        assertEquals(
            listOf(FaceExclusionEntity(7L, "personA", 10L)),
            faceDao.getExclusions()
        )
    }

    @Test
    fun unassignOnlyAffectsTheTargetPersonInSharedMedia() = runBlocking {
        insertPerson("personA")
        insertPerson("personB")
        // One photo, two faces, two different people.
        faceDao.insert(DetectedFaceEntity(mediaId = 7L, personId = "personA", embedding = byteArrayOf(1)))
        faceDao.insert(DetectedFaceEntity(mediaId = 7L, personId = "personB", embedding = byteArrayOf(2)))

        faceDao.unassignPersonMedia("personA", listOf(7L))

        val faces = faceDao.getByMedia(7L)
        assertEquals(2, faces.size)
        assertEquals(setOf(null, "personB"), faces.mapTo(hashSetOf()) { it.personId })
        assertEquals(listOf(7L), faceDao.getMediaIdsForPerson("personB"))
    }

    @Test
    fun observedMediaIdsUpdateAfterUnassign() = runBlocking {
        insertPerson("personA")
        faceDao.insert(DetectedFaceEntity(mediaId = 7L, personId = "personA"))
        faceDao.insert(DetectedFaceEntity(mediaId = 8L, personId = "personA"))

        assertEquals(listOf(7L, 8L), faceDao.observeMediaIdsForPerson("personA").first())
        faceDao.unassignPersonMedia("personA", listOf(7L))
        assertEquals(listOf(8L), faceDao.observeMediaIdsForPerson("personA").first())
    }

    @Test
    fun observedPersonIdsForMediaTrackUnassigns() = runBlocking {
        insertPerson("personA")
        insertPerson("personB")
        faceDao.insert(DetectedFaceEntity(mediaId = 7L, personId = "personA"))
        faceDao.insert(DetectedFaceEntity(mediaId = 7L, personId = "personB"))
        faceDao.insert(DetectedFaceEntity(mediaId = 7L)) // unassigned face — excluded

        assertEquals(
            setOf("personA", "personB"),
            faceDao.observePersonIdsForMedia(7L).first().toSet()
        )
        faceDao.unassignPersonMedia("personA", listOf(7L))
        assertEquals(listOf("personB"), faceDao.observePersonIdsForMedia(7L).first())
        faceDao.unassignPersonMedia("personB", listOf(7L))
        assertEquals(emptyList<String>(), faceDao.observePersonIdsForMedia(7L).first())
    }

    @Test
    fun deletingPersonCascadesExclusionsAndClusters() = runBlocking {
        insertPerson("personA")
        faceDao.insert(DetectedFaceEntity(mediaId = 7L, personId = "personA"))
        faceDao.upsertClusters(listOf(FaceClusterEntity("personA", floatArrayOf(1f), 1, 10L)))
        faceDao.insertExclusions(listOf(FaceExclusionEntity(7L, "personA", 10L)))

        personDao.deleteById("personA")

        assertTrue(faceDao.getExclusions().isEmpty())
        assertTrue(faceDao.getClusters().isEmpty())
        // Face rows survive with a NULL person link (SET_NULL FK).
        assertNull(faceDao.getByMedia(7L).single().personId)
    }

    @Test
    fun orphanExclusionsAreCleanedUpWithMedia() = runBlocking {
        insertPerson("personA")
        insertMediaRow(7L)
        faceDao.insertExclusions(
            listOf(
                FaceExclusionEntity(7L, "personA", 10L),
                FaceExclusionEntity(9L, "personA", 10L) // media 9 does not exist
            )
        )

        assertEquals(1, faceDao.deleteOrphanExclusions())
        assertEquals(listOf(7L), faceDao.getExclusions().map { it.mediaId })
    }

    @Test
    fun insertExclusionsIgnoresDuplicateAssertions() = runBlocking {
        insertPerson("personA")
        faceDao.insertExclusions(listOf(FaceExclusionEntity(7L, "personA", 10L)))
        faceDao.insertExclusions(listOf(FaceExclusionEntity(7L, "personA", 20L)))

        assertEquals(
            listOf(FaceExclusionEntity(7L, "personA", 10L)),
            faceDao.getExclusions()
        )
    }
}
