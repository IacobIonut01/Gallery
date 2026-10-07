package com.dot.gallery.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.data.entity.CloudMediaEntity
import com.dot.gallery.data.entity.CloudMediaSnapshotMapper
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import com.dot.gallery.domain.uri.path





@RunWith(AndroidJUnit4::class)
class CloudMediaSnapshotMapperTest {

    private fun entity(
        remoteId: String,
        serverConfigId: Long = 1L,
        timestamp: Long = 1_700_000_000_000L,
        takenTimestamp: Long? = 1_700_000_000_000L,
    ) = CloudMediaEntity(
        remoteId = remoteId,
        providerType = ProviderType.IMMICH,
        serverConfigId = serverConfigId,
        label = "$remoteId.jpg",
        path = "library/$remoteId.jpg",
        relativePath = "library",
        mimeType = "image/jpeg",
        timestamp = timestamp,
        takenTimestamp = takenTimestamp,
        size = 1024L,
    )

    @Test
    fun syncOnlyChangesReuseTheMappedMediaInstance() {
        val mapper = CloudMediaSnapshotMapper()
        val original = entity("asset-1")
        val first = mapper.map(listOf(original)).single()

        val resynced = original.copy(lastSyncedAt = 123L, city = "Paris", country = "France")
        val second = mapper.map(listOf(resynced)).single()

        assertSame(first, second)
    }

    @Test
    fun presentationChangesProduceEqualButDistinctMedia() {
        val modifications: List<Pair<String, (CloudMediaEntity) -> CloudMediaEntity>> = listOf(
            "label" to { it.copy(label = "renamed.jpg") },
            "path" to { it.copy(path = "other/path.jpg") },
            "relativePath" to { it.copy(relativePath = "other") },
            "fileId" to { it.copy(fileId = "file-2") },
            "favorite" to { it.copy(favorite = !it.favorite) },
            "trashed" to { it.copy(trashed = !it.trashed) },
            "size" to { it.copy(size = it.size + 1) },
            "timestamp" to { it.copy(timestamp = it.timestamp + 60_000L) },
            "takenTimestamp" to { it.copy(takenTimestamp = (it.takenTimestamp ?: 0L) + 60_000L) },
            "mimeType" to { it.copy(mimeType = "image/png") },
            "duration" to { it.copy(duration = "00:05") },
        )
        for ((field, mutate) in modifications) {
            val mapper = CloudMediaSnapshotMapper()
            val original = entity("asset-$field")
            val first = mapper.map(listOf(original)).single()

            val mutated = mutate(original)
            val remapped = mapper.map(listOf(mutated)).single()

            assertNotSame("field $field", first, remapped)
            assertEquals("field $field", mutated.toUriMedia(), remapped)
        }
    }

    @Test
    fun accountsWithTheSameRemoteIdMapIndependently() {
        val mapper = CloudMediaSnapshotMapper()
        val first = entity("shared-remote", serverConfigId = 1L)
        val second = entity("shared-remote", serverConfigId = 2L)

        val mapped = mapper.map(listOf(first, second))

        assertNotEquals(first.globalMediaId, second.globalMediaId)
        assertNotSame(mapped[0], mapped[1])
        assertEquals(first.globalMediaId, mapped[0].id)
        assertEquals(second.globalMediaId, mapped[1].id)
    }

    @Test
    fun removedThenReaddedIdDoesNotReuseRetiredEntry() {
        val mapper = CloudMediaSnapshotMapper()
        val retained = entity("retained")
        val removed = entity("removed")
        val firstPass = mapper.map(listOf(retained, removed))
        val firstRemoved = firstPass.single { it.id == removed.globalMediaId }

        mapper.map(listOf(retained))
        val thirdPass = mapper.map(listOf(retained, removed))
        val readded = thirdPass.single { it.id == removed.globalMediaId }
        val stillRetained = thirdPass.single { it.id == retained.globalMediaId }

        assertNotSame(firstRemoved, readded)
        assertEquals(removed.toUriMedia(), readded)
        assertSame(firstPass.single { it.id == retained.globalMediaId }, stillRetained)
    }

    @Test
    fun edgeTimestampsMatchTheOriginalFormatter() {
        val mapper = CloudMediaSnapshotMapper()
        val zero = entity("zero", timestamp = 0L, takenTimestamp = null)
        val negative = entity("negative", timestamp = -5_000L, takenTimestamp = null)

        val mapped = mapper.map(listOf(zero, negative))

        assertEquals(zero.toUriMedia(), mapped[0])
        assertEquals(negative.toUriMedia(), mapped[1])
        assertEquals(zero.toUriMedia().fullDate, mapped[0].fullDate)
        assertEquals(negative.toUriMedia().fullDate, mapped[1].fullDate)
    }

    @Test
    fun timeZoneChangeReformatsWithoutStaleDates() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val mapper = CloudMediaSnapshotMapper()
            val item = entity("tz")
            val first = mapper.map(listOf(item)).single()

            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
            val second = mapper.map(listOf(item)).single()

            assertNotSame(first, second)
            assertEquals(item.toUriMedia(), second)
            assertNotEquals(first.fullDate, second.fullDate)
        } finally {
            TimeZone.setDefault(previous)
        }
    }

    @Test
    fun unchangedLargeSnapshotReusesEveryInstance() {
        val mapper = CloudMediaSnapshotMapper()
        val source = (1..4_000).map { index -> entity("asset-$index") }

        val first = mapper.map(source)
        val second = mapper.map(source.map { it.copy(lastSyncedAt = it.lastSyncedAt + 1) })

        assertEquals(first.size, second.size)
        first.indices.forEach { index ->
            assertSame(first[index], second[index])
        }
    }
}
