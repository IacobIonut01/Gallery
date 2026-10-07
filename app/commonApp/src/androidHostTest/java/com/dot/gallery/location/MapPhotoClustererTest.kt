package com.dot.gallery.location

import com.dot.gallery.domain.model.locationCoordinateKey
import com.dot.gallery.domain.model.matchesLocationName
import com.dot.gallery.domain.model.locationLabelKey
import com.dot.gallery.domain.model.locationCoordinateGroupKey
import com.dot.gallery.domain.model.matchesLocationCoordinates
import com.dot.gallery.domain.model.locationIdentityKey
import com.dot.gallery.cloud.core.CloudMapMarker
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.feature.library.location.AccountCloudMapMarker
import com.dot.gallery.app.presentation.location.MapGeoBounds
import com.dot.gallery.feature.library.location.buildActionableLocations
import com.dot.gallery.app.presentation.location.MapPhotoClusterer
import com.dot.gallery.app.presentation.location.MapPhotoPoint
import com.dot.gallery.domain.model.LocationMedia
import com.dot.gallery.feature.library.location.locationLabel
import com.dot.gallery.app.presentation.location.mapMediaViewerRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import com.dot.gallery.domain.model.EncryptedMedia
import com.dot.gallery.domain.uri.path





class MapPhotoClustererTest {
    @Test
    fun emptyInputProducesNoClusters() {
        assertTrue(MapPhotoClusterer.cluster(emptyList(), 10.0).isEmpty())
    }

    @Test
    fun nearbyPhotosClusterAndNewestIsRepresentative() {
        val points = listOf(
            point(1, 46.7700, 23.5900, 100),
            point(2, 46.7701, 23.5901, 200),
        )
        val clusters = MapPhotoClusterer.cluster(points, zoom = 12.0)
        assertEquals(1, clusters.size)
        assertEquals(2, clusters.single().count)
        assertEquals(2L, clusters.single().representativeMediaId)
    }

    @Test
    fun distantPhotosRemainSeparate() {
        val clusters = MapPhotoClusterer.cluster(
            listOf(
                point(1, 46.77, 23.59, 100),
                point(2, 40.71, -74.00, 200),
            ),
            zoom = 8.0,
        )
        assertEquals(2, clusters.size)
        assertTrue(clusters.none { it.isCluster })
    }

    @Test
    fun clusterSplitsAsZoomIncreases() {
        val points = listOf(
            point(1, 46.7700, 23.5900, 100),
            point(2, 46.7710, 23.5910, 200),
        )
        assertEquals(1, MapPhotoClusterer.cluster(points, zoom = 10.0).size)
        assertEquals(2, MapPhotoClusterer.cluster(points, zoom = 20.0).size)
    }

    @Test
    fun coLocatedPhotosRemainReachableAsOneCluster() {
        val points = listOf(
            point(1, 46.77, 23.59, 100),
            point(2, 46.77, 23.59, 200),
            point(3, 46.77, 23.59, 300),
        )
        val cluster = MapPhotoClusterer.cluster(points, zoom = 22.0).single()
        assertEquals(listOf(3L, 2L, 1L), cluster.members.map { it.mediaId })
        assertTrue(cluster.isCluster)
    }

    @Test
    fun renderIdIsIndependentOfInputOrder() {
        val points = listOf(
            point(4, 46.77, 23.59, 100),
            point(7, 46.77, 23.59, 200),
            point(9, 46.77, 23.59, 300),
        )
        val first = MapPhotoClusterer.cluster(points, 12.0).single().renderId
        val second = MapPhotoClusterer.cluster(points.reversed(), 12.0).single().renderId
        assertEquals(first, second)
    }

    @Test
    fun antimeridianPointsClusterAcrossWorldWrap() {
        val cluster = MapPhotoClusterer.cluster(
            listOf(
                point(1, 0.0, 179.999, 100),
                point(2, 0.0, -179.999, 200),
            ),
            zoom = 10.0,
        ).single()
        assertEquals(2, cluster.count)
        assertTrue(cluster.bounds.crossesAntimeridian)
        assertTrue(kotlin.math.abs(cluster.longitude) > 179.0)
    }

    @Test
    fun visibleSelectionHonorsBoundsAndLimit() {
        val clusters = MapPhotoClusterer.cluster(
            listOf(
                point(1, 46.77, 23.59, 300),
                point(2, 40.71, -74.00, 200),
                point(3, 35.67, 139.65, 100),
            ),
            zoom = 12.0,
        )
        val visible = MapPhotoClusterer.visible(
            clusters = clusters,
            bounds = MapGeoBounds(45.0, 22.0, 48.0, 25.0),
            limit = 1,
            centerLatitude = 46.77,
            centerLongitude = 23.59,
        )
        assertEquals(listOf(1L), visible.map { it.representativeMediaId })
        assertFalse(visible.single().isCluster)
    }

    @Test
    fun cloudMarkerKeysIncludeOwningAccount() {
        val marker = CloudMapMarker(
            latitude = 46.77,
            longitude = 23.59,
            assetId = "shared-asset-id",
            providerType = ProviderType.IMMICH,
        )

        assertNotEquals(
            AccountCloudMapMarker(configId = 11L, marker = marker).key,
            AccountCloudMapMarker(configId = 22L, marker = marker).key,
        )
    }

    @Test
    fun mapMediaRouteOpensViewerWithoutLocationTimelineParent() {
        val route = mapMediaViewerRoute(-42L)

        assertEquals("media_screen?mediaId=-42&albumId=-1&slideshow=false", route)
        assertFalse(route.contains("location_timeline_screen"))
    }

    @Test
    fun visuallyIdenticalLocationLabelsShareOneIdentity() {
        assertEquals(
            locationLabelKey("Costinesti, Romania"),
            locationLabelKey(" costinesti,\u00A0ROMANIA "),
        )
        assertTrue(matchesLocationName(null, "Costinesti, Romania", "Costinesti", "Romania"))
        assertTrue(matchesLocationName("", "Costinesti, Romania", "Costinesti", "Romania"))
        assertTrue(matchesLocationName("\u00A0", "Costinesti, Romania", "Costinesti", "Romania"))
        assertTrue(matchesLocationName("Costinesti, Romania", "", "Costinesti", "Romania"))
    }

    @Test
    fun partialLocationNameMatchesAvailableComponents() {
        assertTrue(matchesLocationName("Cardiff", "United Kingdom", "", "united kingdom"))
        assertTrue(matchesLocationName("Bristol", "United Kingdom", " bristol ", ""))
        assertFalse(matchesLocationName("Bristol", "United Kingdom", "", ""))
    }

    @Test
    fun zeroCoordinatesRemainAValidTimelineIdentity() {
        assertTrue(matchesLocationCoordinates(0.0, 0.0, 0.0, 0.0))
        assertEquals(locationCoordinateKey(-0.0, -0.0), locationCoordinateKey(0.0, 0.0))
        assertFalse(matchesLocationCoordinates(0.0, 1.0, 0.0, 0.0))
    }

    @Test
    fun coordinatesThatRoundToTheSameLabelShareAGroupButKeepDistinctIdentity() {
        val first = locationMedia(1L, "17.8490, 73.8034", 17.84901, 73.80341)
        val second = locationMedia(2L, "17.8490, 73.8034", 17.84902, 73.80342)

        assertNotEquals(first.locationIdentityKey(), second.locationIdentityKey())
        assertEquals(
            locationCoordinateGroupKey(first.latitude, first.longitude),
            locationCoordinateGroupKey(second.latitude, second.longitude),
        )
        assertTrue(
            matchesLocationCoordinates(
                first.latitude,
                first.longitude,
                second.latitude,
                second.longitude,
            )
        )
        assertFalse(matchesLocationCoordinates(17.84901, 73.80341, 17.8492, 73.8036))
    }

    @Test
    fun zeroUnnamedLocationFallsBackToUnknownLocationLabel() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)
        try {
            assertEquals("Unknown Location", locationLabel(null, null, 0.0, 0.0))
            assertEquals("Unknown Location", locationLabel(null, null, -0.0, -0.0))
            assertFalse(locationLabel(null, null, 0.0, -0.0).contains("0,0000"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun namedZeroCoordinatesKeepTheirActualName() {
        assertEquals(
            "Null Island, Nowhere",
            locationLabel("Null Island", "Nowhere", 0.0, 0.0),
        )
        assertEquals("United Kingdom", locationLabel(null, "United Kingdom", 0.0, 0.0))
    }

    @Test
    fun singleAxisZeroStillRendersCoordinates() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            assertEquals("0.0000, 1.0000", locationLabel(null, null, 0.0, 1.0))
            assertEquals("1.0000, 0.0000", locationLabel(null, null, 1.0, 0.0))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun invalidOrMissingCoordinatesReturnInjectedFallback() {
        assertEquals("Unavailable", locationLabel(null, null, null, null, "Unavailable"))
        assertEquals("Unavailable", locationLabel(null, null, 91.0, 0.0, "Unavailable"))
        assertEquals("Unavailable", locationLabel(null, null, 0.0, 181.0, "Unavailable"))
        assertEquals("Unavailable", locationLabel(null, null, Double.NaN, 0.0, "Unavailable"))
    }

    @Test
    fun coordinateOnlyMediaWithTheSameDisplayedLocationProduceOneCard() {
        val first = locationMedia(1L, "36.4241, 32.1581", 36.42411, 32.15811)
        val second = locationMedia(2L, "36.4241, 32.1581", 36.42412, 32.15812)
        val locations = buildActionableLocations(
            localLocations = listOf(first, second),
            geoMedia = emptyList(),
            cachedCloudMedia = emptyList(),
        )

        assertEquals(1, locations.size)
    }

    private fun point(id: Long, latitude: Double, longitude: Double, timestamp: Long) =
        MapPhotoPoint(id, latitude, longitude, timestamp)

    private fun locationMedia(id: Long, label: String, latitude: Double, longitude: Double) = LocationMedia(
        media = EncryptedMedia(
            id = id,
            label = label,
            bytes = byteArrayOf(),
            path = "",
            relativePath = "",
            albumID = 0L,
            albumLabel = "",
            timestamp = 0L,
            fullDate = "",
            mimeType = "image/jpeg",
            favorite = 0,
            trashed = 0,
            size = 0L,
        ),
        location = label,
        latitude = latitude,
        longitude = longitude,
    )
}
