/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.date

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

fun Long.epochSecToLocalDate(): LocalDate =
    Instant.fromEpochSeconds(this).toLocalDateTime(TimeZone.currentSystemDefault()).date

fun todayLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun Long.formatMinSec(): String =
    if (this == 0L) {
        "00:00"
    } else {
        val totalMinutes = this / 60_000L
        val seconds = this % 60_000L / 1_000L
        totalMinutes.toString().padStart(2, '0') + ":" +
            seconds.toString().padStart(2, '0')
    }

fun String?.formatMinSec(): String {
    if (this == null) return ""
    // Plain numeric millis first
    toLongOrNull()?.let { return it.formatMinSec() }
    // HH:MM:SS.mmm or MM:SS.mmm format from cloud providers
    return try {
        val parts = split(":")
        if (parts.size >= 2) {
            val secParts = parts.last().split(".")
            val seconds = secParts[0].toLong()
            val millis = if (secParts.size > 1) secParts[1].toLong() else 0L
            val minutes = parts[parts.size - 2].toLong()
            val hours = if (parts.size >= 3) parts[parts.size - 3].toLong() else 0L
            ((hours * 3600 + minutes * 60 + seconds) * 1000 + millis).formatMinSec()
        } else ""
    } catch (_: Exception) {
        ""
    }
}

private val FILENAME_DATE_REGEX =
    Regex("""(?<!\d)(\d{4})(\d{2})(\d{2})[_\-](\d{2})(\d{2})(\d{2})(?!\d)""")

/**
 * Attempts to parse a timestamp (epoch millis) from common camera filename
 * patterns (IMG_20180508_213737.jpg, VID_20180508_213737.mp4, ...).
 * Returns null if the filename does not match a known date pattern.
 */
fun String.parseTimestampFromFilename(): Long? {
    val match = FILENAME_DATE_REGEX.find(this) ?: return null
    val (year, month, day, hour, minute, second) = match.destructured
    return try {
        val y = year.toInt()
        val m = month.toInt()
        val d = day.toInt()
        if (y < 1970 || y > 2100 || m < 1 || m > 12 || d < 1 || d > 31) return null
        val millis = LocalDateTime(y, m, d, hour.toInt(), minute.toInt(), second.toInt())
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        // A photo cannot be taken in the future. Filename parsing is heuristic
        // and may latch onto numeric IDs (e.g. Facebook exports), so discard
        // future-dated results.
        if (millis > Clock.System.now().toEpochMilliseconds()) return null
        millis
    } catch (_: Exception) {
        null
    }
}
