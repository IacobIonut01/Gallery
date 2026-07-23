package com.dot.gallery.feature_node.presentation.location

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

data class MapPhotoPoint(
    val mediaId: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
)

data class MapGeoBounds(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
    val crossesAntimeridian: Boolean = false,
) {
    fun contains(latitude: Double, longitude: Double, paddingDegrees: Double = 0.0): Boolean {
        val inLatitude = latitude in (south - paddingDegrees)..(north + paddingDegrees)
        val normalized = normalizeLongitude(longitude)
        val inLongitude = if (crossesAntimeridian) {
            normalized >= west - paddingDegrees || normalized <= east + paddingDegrees
        } else {
            normalized in (west - paddingDegrees)..(east + paddingDegrees)
        }
        return inLatitude && inLongitude
    }
}

data class MapPhotoCluster(
    val renderId: String,
    val latitude: Double,
    val longitude: Double,
    val representativeMediaId: Long,
    val members: List<MapPhotoPoint>,
    val bounds: MapGeoBounds,
) {
    val count: Int get() = members.size
    val isCluster: Boolean get() = members.size > 1
}

object MapPhotoClusterer {
    fun cluster(
        points: List<MapPhotoPoint>,
        zoom: Double,
        radiusPx: Double = radiusForZoom(zoom),
    ): List<MapPhotoCluster> {
        if (points.isEmpty()) return emptyList()
        val safeZoom = zoom.coerceIn(0.0, 22.0)
        val worldSize = 512.0 * 2.0.pow(safeZoom)
        val radius = radiusPx.coerceAtLeast(1.0)
        val bucketCount = ceil(worldSize / radius).toInt().coerceAtLeast(1)
        val projected = points
            .filter { it.latitude.isFinite() && it.longitude.isFinite() }
            .sortedBy { it.mediaId }
            .map { ProjectedPoint(it, worldX(it.longitude, worldSize), worldY(it.latitude, worldSize)) }
        if (projected.isEmpty()) return emptyList()

        val buckets = HashMap<Long, MutableList<Int>>()
        projected.forEachIndexed { index, point ->
            val bx = floor(point.x / radius).toInt().mod(bucketCount)
            val by = floor(point.y / radius).toInt().coerceIn(0, bucketCount - 1)
            buckets.getOrPut(bucketKey(bx, by)) { ArrayList() }.add(index)
        }

        val parent = IntArray(projected.size) { it }
        fun find(start: Int): Int {
            var node = start
            while (parent[node] != node) {
                parent[node] = parent[parent[node]]
                node = parent[node]
            }
            return node
        }
        fun union(a: Int, b: Int) {
            val rootA = find(a)
            val rootB = find(b)
            if (rootA != rootB) {
                if (rootA < rootB) parent[rootB] = rootA else parent[rootA] = rootB
            }
        }

        projected.forEachIndexed { index, point ->
            val bx = floor(point.x / radius).toInt().mod(bucketCount)
            val by = floor(point.y / radius).toInt().coerceIn(0, bucketCount - 1)
            for (dx in -1..1) {
                val nx = (bx + dx).mod(bucketCount)
                for (dy in -1..1) {
                    val ny = by + dy
                    if (ny !in 0 until bucketCount) continue
                    buckets[bucketKey(nx, ny)].orEmpty().forEach { other ->
                        if (other <= index) return@forEach
                        val candidate = projected[other]
                        val rawDx = abs(point.x - candidate.x)
                        val wrappedDx = minOf(rawDx, worldSize - rawDx)
                        val distanceSquared = wrappedDx * wrappedDx + (point.y - candidate.y) * (point.y - candidate.y)
                        if (distanceSquared <= radius * radius) union(index, other)
                    }
                }
            }
        }

        val groups = LinkedHashMap<Int, MutableList<ProjectedPoint>>()
        projected.indices.forEach { index ->
            groups.getOrPut(find(index)) { ArrayList() }.add(projected[index])
        }
        return groups.values
            .map { createCluster(it, worldSize) }
            .sortedWith(compareByDescending<MapPhotoCluster> { it.members.maxOf(MapPhotoPoint::timestamp) }.thenBy { it.renderId })
    }

    fun visible(
        clusters: List<MapPhotoCluster>,
        bounds: MapGeoBounds?,
        limit: Int,
        centerLatitude: Double,
        centerLongitude: Double,
    ): List<MapPhotoCluster> {
        val candidates = if (bounds == null) clusters else clusters.filter { cluster ->
            bounds.contains(cluster.latitude, cluster.longitude, paddingDegrees = 0.5)
        }
        return candidates
            .sortedBy { cluster ->
                val dx = shortestLongitudeDistance(cluster.longitude, centerLongitude)
                val dy = cluster.latitude - centerLatitude
                dx * dx + dy * dy
            }
            .take(limit.coerceAtLeast(1))
    }

    fun radiusForZoom(zoom: Double): Double = when {
        zoom >= 18.0 -> 24.0
        zoom >= 16.0 -> 36.0
        zoom >= 14.0 -> 48.0
        else -> 64.0
    }

    private fun createCluster(points: List<ProjectedPoint>, worldSize: Double): MapPhotoCluster {
        val anchorX = points.first().x
        val adjustedXs = points.map { point ->
            when {
                point.x - anchorX > worldSize / 2.0 -> point.x - worldSize
                anchorX - point.x > worldSize / 2.0 -> point.x + worldSize
                else -> point.x
            }
        }
        val centerX = adjustedXs.average().mod(worldSize)
        val centerY = points.map { it.y }.average()
        val members = points.map { it.source }.sortedByDescending { it.timestamp }
        val representative = members.first()
        return MapPhotoCluster(
            renderId = stableRenderId(members),
            latitude = latitudeFromWorldY(centerY, worldSize),
            longitude = longitudeFromWorldX(centerX, worldSize),
            representativeMediaId = representative.mediaId,
            members = members,
            bounds = boundsOf(members),
        )
    }

    private fun boundsOf(points: List<MapPhotoPoint>): MapGeoBounds {
        val south = points.minOf { it.latitude }.coerceIn(-85.05112878, 85.05112878)
        val north = points.maxOf { it.latitude }.coerceIn(-85.05112878, 85.05112878)
        val longitudes = points.map { normalizeLongitude(it.longitude) }.sorted()
        if (longitudes.size == 1) {
            return MapGeoBounds(south, longitudes.first(), north, longitudes.first())
        }
        var largestGap = -1.0
        var gapIndex = 0
        for (index in longitudes.indices) {
            val current = longitudes[index]
            val next = if (index == longitudes.lastIndex) longitudes.first() + 360.0 else longitudes[index + 1]
            val gap = next - current
            if (gap > largestGap) {
                largestGap = gap
                gapIndex = index
            }
        }
        val west = longitudes[(gapIndex + 1) % longitudes.size]
        val east = longitudes[gapIndex]
        val crosses = west > east
        return MapGeoBounds(south, west, north, east, crosses)
    }

    private fun stableRenderId(points: List<MapPhotoPoint>): String {
        var hash = -0x340d631b7bdddcdbL
        points.map { it.mediaId }.sorted().forEach { id ->
            hash = (hash xor id) * 0x100000001b3L
        }
        return "photo-${points.size}-${hash.toULong().toString(16)}"
    }

    private fun worldX(longitude: Double, worldSize: Double): Double =
        ((normalizeLongitude(longitude) + 180.0) / 360.0 * worldSize).mod(worldSize)

    private fun worldY(latitude: Double, worldSize: Double): Double {
        val safeLatitude = latitude.coerceIn(-85.05112878, 85.05112878)
        val radians = Math.toRadians(safeLatitude)
        return ((1.0 - asinh(tan(radians)) / PI) / 2.0 * worldSize).coerceIn(0.0, worldSize)
    }

    private fun longitudeFromWorldX(x: Double, worldSize: Double): Double =
        normalizeLongitude(x / worldSize * 360.0 - 180.0)

    private fun latitudeFromWorldY(y: Double, worldSize: Double): Double =
        Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * y / worldSize))))

    private fun shortestLongitudeDistance(a: Double, b: Double): Double {
        val raw = abs(normalizeLongitude(a) - normalizeLongitude(b))
        return minOf(raw, 360.0 - raw)
    }

    private fun bucketKey(x: Int, y: Int): Long = (x.toLong() shl 32) xor (y.toLong() and 0xffffffffL)

    private data class ProjectedPoint(val source: MapPhotoPoint, val x: Double, val y: Double)
}

internal fun normalizeLongitude(longitude: Double): Double =
    ((longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
