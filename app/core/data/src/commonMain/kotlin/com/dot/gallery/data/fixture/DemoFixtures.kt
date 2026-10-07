/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.fixture

import com.dot.gallery.data.entity.SmartScanPhase
import com.dot.gallery.data.entity.SmartScanPhaseEntity
import com.dot.gallery.data.entity.SmartScanRunEntity
import com.dot.gallery.data.entity.SmartScanStatus
import com.dot.gallery.data.entity.SmartScanTrigger
import com.dot.gallery.domain.date.getDate
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.domain.uri.parseUri

/**
 * Canonical demo data for docs scenes and wasmJs fixture repositories.
 *
 * Scenes should prefer these over hand-rolled objects so every screen renders
 * the same deterministic library — the same set K4 feature ports will consume.
 * All values are pure Kotlin; no assets, no I/O, safe on every target.
 */
object DemoFixtures {

    private const val HOUR = 3_600L
    private const val DAY = 86_400L

    private data class FixtureSeed(
        val label: String,
        val mimeType: String,
        val ageSeconds: Long,
        val albumLabel: String,
        val albumId: Long,
        val sizeKb: Long,
        val duration: String? = null,
        val favorite: Boolean = false,
    )

    private val seeds = listOf(
        FixtureSeed("IMG_0001.jpg", "image/jpeg", 1 * HOUR, "Camera", 1L, 2_400, favorite = true),
        FixtureSeed("IMG_0002.jpg", "image/jpeg", 3 * HOUR, "Camera", 1L, 1_900),
        FixtureSeed("IMG_0003.jpg", "image/jpeg", 26 * HOUR, "Camera", 1L, 2_100),
        FixtureSeed("IMG_0004.jpg", "image/jpeg", 4 * DAY, "Camera", 1L, 2_800),
        FixtureSeed("VID_0001.mp4", "video/mp4", 6 * DAY, "Camera", 1L, 18_000, duration = "00:42"),
        FixtureSeed("IMG_0005.png", "image/png", 9 * DAY, "Screenshots", 2L, 310),
        FixtureSeed("IMG_0006.png", "image/png", 12 * DAY, "Screenshots", 2L, 280),
        FixtureSeed("IMG_0007.jpg", "image/jpeg", 32 * DAY, "Download", 3L, 1_450),
        FixtureSeed("VID_0002.webm", "video/webm", 60 * DAY, "Download", 3L, 9_400, duration = "01:17"),
        FixtureSeed("IMG_0008.jpg", "image/jpeg", 410 * DAY, "Camera", 1L, 2_200),
    )

    /**
     * Deterministic media set anchored to [nowSec] (defaults chosen so callers
     * pass `Clock.System.now().epochSeconds`). Dates spread across today /
     * this week / this month / last year so date-grouping scenes exercise
     * every header bucket.
     */
    fun media(nowSec: Long): List<Media> = seeds.mapIndexed { index, seed ->
        val taken = nowSec - seed.ageSeconds
        UriMedia(
            id = (index + 1).toLong(),
            label = seed.label,
            uri = parseUri("fixture://media/${seed.label}"),
            path = "/demo/${seed.albumLabel}/${seed.label}",
            relativePath = "demo/${seed.albumLabel}/",
            albumID = seed.albumId,
            albumLabel = seed.albumLabel,
            timestamp = taken,
            takenTimestamp = taken,
            fullDate = taken.getDate("EEE, MMM d, yyyy"),
            mimeType = seed.mimeType,
            favorite = if (seed.favorite) 1 else 0,
            trashed = 0,
            size = seed.sizeKb * 1024L,
            duration = seed.duration,
        )
    }

    /** Albums derived from [media]'s album groupings — count/size consistent. */
    fun albums(nowSec: Long): List<Album> = media(nowSec)
        .groupBy { it.albumID }
        .map { (albumId, items) ->
            Album(
                id = albumId,
                label = items.first().albumLabel,
                uri = parseUri("fixture://album/$albumId"),
                pathToThumbnail = items.maxBy { it.timestamp }.path,
                relativePath = items.first().relativePath,
                timestamp = items.maxOf { it.timestamp },
                count = items.size.toLong(),
                size = items.sumOf { it.size },
            )
        }
        .sortedByDescending { it.timestamp }

    /**
     * A mid-flight Smart Features scan — phase 3 of 6 running — for settings
     * scenes that render live progress.
     */
    fun smartScanRun(nowSec: Long): SmartScanRunEntity = SmartScanRunEntity(
        runId = "demo-run",
        trigger = SmartScanTrigger.MANUAL,
        userVisible = true,
        status = SmartScanStatus.RUNNING,
        requestedAt = (nowSec - 4 * 60) * 1_000,
        startedAt = (nowSec - 3 * 60) * 1_000,
        updatedAt = nowSec * 1_000,
        currentPhase = SmartScanPhase.SEARCH_INDEX,
        totalMedia = 4_800,
        processedMedia = 2_016,
        succeededMedia = 1_980,
        skippedMedia = 24,
        failedMedia = 12,
    )

    /** Phase rows matching [smartScanRun] — first two done, third running. */
    fun smartScanPhases(nowSec: Long): List<SmartScanPhaseEntity> {
        val run = smartScanRun(nowSec)
        fun phase(
            phase: SmartScanPhase,
            status: SmartScanStatus,
            total: Int = 4_800,
            processed: Int = total,
        ) = SmartScanPhaseEntity(
            runId = run.runId,
            phase = phase,
            status = status,
            startedAt = run.startedAt,
            updatedAt = run.updatedAt,
            totalMedia = total,
            processedMedia = processed,
        )
        return listOf(
            phase(SmartScanPhase.SOURCE_SYNC, SmartScanStatus.SUCCEEDED),
            phase(SmartScanPhase.METADATA, SmartScanStatus.SUCCEEDED),
            phase(SmartScanPhase.SEARCH_INDEX, SmartScanStatus.RUNNING, processed = 2_016),
            phase(SmartScanPhase.CATEGORY_CLASSIFICATION, SmartScanStatus.QUEUED, processed = 0),
            phase(SmartScanPhase.FACE_INDEX, SmartScanStatus.QUEUED, processed = 0),
            phase(SmartScanPhase.FACE_CLUSTER, SmartScanStatus.QUEUED, processed = 0),
        )
    }
}
