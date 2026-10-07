package com.dot.gallery.cloud

import android.os.Looper
import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.data.InternalDatabase
import com.dot.gallery.domain.model.GeoMedia
import com.dot.gallery.domain.model.LocationMedia
import com.dot.gallery.domain.model.MediaState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.feature.library.location.MapGeoMediaSource



@RunWith(AndroidJUnit4::class)
class CloudBackupResponsivenessTest {

    private lateinit var db: InternalDatabase
    private lateinit var source: MapGeoMediaSource

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        source = MapGeoMediaSource(
            ProviderRegistry(),
            db.getCloudMediaDao(),
            db.getCloudServerConfigDao(),
            context
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun mergedGeoMediaRunsUpstreamOffTheMainThread() = runTest {
        val localGeoMedia = flow<List<GeoMedia>> {
            assertFalse(Looper.myLooper() == Looper.getMainLooper())
            emit(emptyList())
        }

        val merged = withContext(Dispatchers.Main) {
            source.mergedGeoMedia(
                localGeoMedia = localGeoMedia,
                timelineMedia = flowOf(MediaState<UriMedia>())
            ).first()
        }

        assertEquals(emptyList<GeoMedia>(), merged)
    }

    @Test
    fun mergedLocationsRunsUpstreamOffTheMainThread() = runTest {
        val localLocations = flow<List<LocationMedia>> {
            assertFalse(Looper.myLooper() == Looper.getMainLooper())
            emit(emptyList())
        }

        val merged = withContext(Dispatchers.Main) {
            source.mergedLocations(
                localLocations = localLocations,
                geoMedia = flowOf(emptyList<GeoMedia>()),
                timelineMedia = flowOf(MediaState<UriMedia>())
            ).first()
        }

        assertEquals(emptyList<LocationMedia>(), merged)
    }

    @Test
    fun mergedGeoMediaCancelsUpstreamWhenTheCollectorLeaves() = runTest {
        val upstreamCancelled = CompletableDeferred<Unit>()
        val localGeoMedia = flow<List<GeoMedia>> {
            try {
                emit(emptyList())
                awaitCancellation()
            } finally {
                upstreamCancelled.complete(Unit)
            }
        }

        withContext(Dispatchers.Main) {
            source.mergedGeoMedia(
                localGeoMedia = localGeoMedia,
                timelineMedia = flowOf(MediaState<UriMedia>())
            ).first()
        }

        withTimeout(5_000L) { upstreamCancelled.await() }
        assertTrue(upstreamCancelled.isCompleted)
    }
}
