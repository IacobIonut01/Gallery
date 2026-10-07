/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.domain.model

import com.dot.gallery.domain.model.timestampFor
import com.dot.gallery.domain.model.parseCaptureTimestamp
import com.dot.gallery.domain.util.MediaOrder
import com.dot.gallery.domain.util.OrderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import com.dot.gallery.domain.model.EncryptedMedia
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.TimelineDateSource
import com.dot.gallery.domain.uri.path


class MediaCaptureTimeTest {

    @Test
    fun exifDateWithoutOffset_usesSuppliedLocalZone() {
        val zone = ZoneId.of("Europe/Bucharest")

        val timestamp = parseCaptureTimestamp("2024:07:14 18:30:15", defaultZone = zone)

        assertEquals(
            LocalDateTime.of(2024, 7, 14, 18, 30, 15).atZone(zone).toInstant().toEpochMilli(),
            timestamp
        )
    }

    @Test
    fun exifDateWithOffset_usesEmbeddedOffset() {
        val timestamp = parseCaptureTimestamp(
            rawValue = "2024:07:14 18:30:15",
            offsetValue = "+03:00",
            defaultZone = ZoneOffset.UTC
        )

        assertEquals(
            LocalDateTime.of(2024, 7, 14, 18, 30, 15)
                .toInstant(ZoneOffset.ofHours(3)).toEpochMilli(),
            timestamp
        )
    }

    @Test
    fun compactVideoDate_parsesUtcTimestamp() {
        val timestamp = parseCaptureTimestamp("20240714T153015.250Z")

        assertEquals(1_720_971_015_250L, timestamp)
    }

    @Test
    fun preEpochCaptureDate_isPreserved() {
        val timestamp = parseCaptureTimestamp(
            rawValue = "1965:01:02 03:04:05",
            defaultZone = ZoneOffset.UTC
        )

        assertNotNull(timestamp)
        assertEquals(
            LocalDateTime.of(1965, 1, 2, 3, 4, 5).toInstant(ZoneOffset.UTC).toEpochMilli(),
            timestamp
        )
    }

    @Test
    fun malformedCaptureDate_returnsNull() {
        assertEquals(null, parseCaptureTimestamp("not-a-date"))
        assertEquals(null, parseCaptureTimestamp(""))
        assertEquals(null, parseCaptureTimestamp(null))
    }

    @Test
    fun timelineDateSource_selectsCaptureOrModifiedTimestamp() {
        val media = media(id = 1L, modifiedSeconds = 200L, captureMillis = 100_000L)

        assertEquals(100L, media.timestampFor(TimelineDateSource.CAPTURE_TIME))
        assertEquals(200L, media.timestampFor(TimelineDateSource.MODIFIED_TIME))
    }

    @Test
    fun mediaOrder_usesStableIdTieBreaker() {
        val lowerId = media(id = 1L, modifiedSeconds = 200L, captureMillis = 100_000L)
        val higherId = media(id = 2L, modifiedSeconds = 200L, captureMillis = 100_000L)

        assertEquals(
            listOf(lowerId, higherId),
            MediaOrder.Date(OrderType.Ascending).sortMedia(listOf(higherId, lowerId))
        )
        assertEquals(
            listOf(higherId, lowerId),
            MediaOrder.Date(OrderType.Descending).sortMedia(listOf(lowerId, higherId))
        )
    }

    private fun media(
        id: Long,
        modifiedSeconds: Long,
        captureMillis: Long
    ) = EncryptedMedia(
        id = id,
        label = "media-$id",
        bytes = byteArrayOf(),
        path = "/media/$id",
        relativePath = "/media",
        albumID = 1L,
        albumLabel = "Media",
        timestamp = modifiedSeconds,
        takenTimestamp = captureMillis,
        fullDate = "",
        mimeType = "image/jpeg",
        favorite = 0,
        trashed = 0,
        size = 0L
    )
}
