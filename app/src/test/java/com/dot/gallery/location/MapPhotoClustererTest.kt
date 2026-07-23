package com.dot.gallery.location

import com.dot.gallery.feature_node.presentation.location.MapGeoBounds
import com.dot.gallery.feature_node.presentation.location.MapPhotoClusterer
import com.dot.gallery.feature_node.presentation.location.MapPhotoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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

    private fun point(id: Long, latitude: Double, longitude: Double, timestamp: Long) =
        MapPhotoPoint(id, latitude, longitude, timestamp)
}
