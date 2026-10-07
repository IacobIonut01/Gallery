/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.logging

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.dot.gallery.domain.date.formatEpochSec
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Composable
fun logLevelColor(level: LogLevel): Color = when (level) {
    LogLevel.VERBOSE, LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
    LogLevel.INFO -> MaterialTheme.colorScheme.primary
    LogLevel.WARN -> MaterialTheme.colorScheme.tertiary
    LogLevel.ERROR, LogLevel.CRASH -> MaterialTheme.colorScheme.error
    LogLevel.EVENT -> MaterialTheme.colorScheme.secondary
}

/** `HH:mm:ss` in the local timezone — the row timestamp used by log entries. */
fun formatLogRowTime(ts: Long): String {
    val time = Instant.fromEpochMilliseconds(ts)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${time.hour.pad2()}:${time.minute.pad2()}:${time.second.pad2()}"
}

/** `HH:mm` in the local timezone — the compact timestamp on area group rows. */
fun formatLogClock(ts: Long): String {
    val time = Instant.fromEpochMilliseconds(ts)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${time.hour.pad2()}:${time.minute.pad2()}"
}

/** `MMM d, HH:mm:ss.SSS` in the local timezone — the detail-sheet timestamp. */
fun formatLogDetailTime(ts: Long): String {
    val ms = ((ts % 1000) + 1000) % 1000
    return (ts / 1000).formatEpochSec("MMM d, HH:mm:ss") +
            "." + ms.toString().padStart(3, '0')
}

/**
 * Parses a `yyyyMMdd-HHmmss` session id into epoch millis (local timezone), or
 * `null` when the id does not match the expected shape.
 */
fun parseLogSessionTs(sessionId: String): Long? {
    if (sessionId.length != 15 || sessionId[8] != '-') return null
    val parts = runCatching {
        LocalDateTime(
            year = sessionId.substring(0, 4).toInt(),
            monthNumber = sessionId.substring(4, 6).toInt(),
            dayOfMonth = sessionId.substring(6, 8).toInt(),
            hour = sessionId.substring(9, 11).toInt(),
            minute = sessionId.substring(11, 13).toInt(),
            second = sessionId.substring(13, 15).toInt(),
        ).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }.getOrNull()
    return parts
}

private fun Int.pad2(): String = toString().padStart(2, '0')
