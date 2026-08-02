/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import com.dot.gallery.feature_node.data.data_source.MediaFeature
import com.dot.gallery.feature_node.data.data_source.MediaFeatureStateEntity
import com.dot.gallery.feature_node.data.data_source.MediaFeatureStatus
import com.dot.gallery.feature_node.data.data_source.SmartScanDao
import com.dot.gallery.feature_node.data.data_source.SmartScanPhase
import com.dot.gallery.feature_node.data.data_source.SmartScanPhaseEntity
import com.dot.gallery.feature_node.data.data_source.SmartScanRunEntity
import com.dot.gallery.feature_node.data.data_source.SmartScanStatus
import com.dot.gallery.feature_node.data.data_source.SmartScanTrigger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmartScanDaoTest {
    private lateinit var db: InternalDatabase
    private lateinit var dao: SmartScanDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.getSmartScanDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun activeAndLatestFlowsTrackRunLifecycle() = runBlocking {
        dao.upsertRun(SmartScanRunEntity("old", SmartScanTrigger.AUTOMATIC, requestedAt = 10L))
        dao.upsertRun(SmartScanRunEntity("new", SmartScanTrigger.MANUAL, requestedAt = 20L))

        assertEquals("new", dao.observeActiveRun().first()?.runId)
        assertEquals("new", dao.observeLatestRun().first()?.runId)
        assertEquals(2, dao.observeActiveRuns().first().size)

        assertEquals(1, dao.claimRunLease("new", "worker-a", 30L, 40L))
        assertEquals(0, dao.claimRunLease("new", "worker-b", 35L, 45L))
        assertEquals(1, dao.recoverExpiredRunLeases(41L))
        assertEquals(1, dao.claimRunLease("new", "worker-b", 41L, 51L))
        assertEquals(1, dao.finishRun("new", SmartScanStatus.SUCCEEDED, 52L))

        assertEquals("old", dao.observeActiveRun().first()?.runId)
        assertEquals("new", dao.observeLatestRun().first()?.runId)
    }

    @Test
    fun featureLeaseIsExclusiveAndRecoverable() = runBlocking {
        dao.upsertFeatureState(
            MediaFeatureStateEntity(
                mediaId = 7L,
                feature = MediaFeature.SEARCH_EMBEDDING,
                sourceRevision = "source-1",
                updatedAt = 10L
            )
        )

        assertEquals(1, dao.claimFeatureLease(7L, MediaFeature.SEARCH_EMBEDDING, "run", "a", 11L, 20L))
        assertEquals(0, dao.claimFeatureLease(7L, MediaFeature.SEARCH_EMBEDDING, "run", "b", 12L, 21L))
        assertEquals(1, dao.recoverExpiredFeatureLeases(20L))
        assertEquals(1, dao.claimFeatureLease(7L, MediaFeature.SEARCH_EMBEDDING, "run", "b", 21L, 30L))
        assertEquals(
            1,
            dao.finishFeature(
                7L,
                MediaFeature.SEARCH_EMBEDDING,
                "b",
                MediaFeatureStatus.SUCCEEDED,
                "model-v1",
                22L
            )
        )

        val state = dao.getFeatureState(7L, MediaFeature.SEARCH_EMBEDDING)
        assertEquals(MediaFeatureStatus.SUCCEEDED, state?.status)
        assertEquals("model-v1", state?.resultRevision)
        assertNull(state?.leaseOwner)
    }

    @Test
    fun pruningIsBoundedAndCascadesPhases() = runBlocking {
        repeat(4) { index ->
            val runId = "run-$index"
            dao.upsertRun(
                SmartScanRunEntity(
                    runId = runId,
                    trigger = SmartScanTrigger.AUTOMATIC,
                    status = SmartScanStatus.SUCCEEDED,
                    requestedAt = index.toLong(),
                    finishedAt = index.toLong(),
                    updatedAt = index.toLong()
                )
            )
            dao.upsertPhase(
                SmartScanPhaseEntity(
                    runId = runId,
                    phase = SmartScanPhase.METADATA,
                    status = SmartScanStatus.SUCCEEDED,
                    updatedAt = index.toLong()
                )
            )
        }

        assertEquals(2, dao.pruneTerminalRuns(keepLatest = 1, maxDelete = 2))
        assertEquals("run-3", dao.getRun("run-3")?.runId)
        assertNull(dao.getRun("run-1"))
        assertEquals(0, dao.observePhases("run-1").first().size)
        assertEquals(1, dao.pruneTerminalRuns(keepLatest = 1, maxDelete = 10))
        assertEquals("run-3", dao.observeLatestRun().first()?.runId)
    }
}
