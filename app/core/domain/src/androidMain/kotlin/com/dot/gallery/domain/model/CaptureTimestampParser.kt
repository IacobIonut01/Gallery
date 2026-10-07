/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val exifDateTimeFormatter =
    DateTimeFormatter.ofPattern("uuuu:MM:dd HH:mm:ss", Locale.ROOT)
private val dashedDateTimeFormatter =
    DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss", Locale.ROOT)
private val compactVideoDateTimeFormatter =
    DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss", Locale.ROOT)
private val compactVideoMillisFormatter =
    DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss.SSS", Locale.ROOT)

fun parseCaptureTimestamp(
    rawValue: String?,
    offsetValue: String? = null,
    defaultZone: ZoneId = ZoneId.systemDefault()
): Long? {
    val value = rawValue?.trim()?.takeIf(String::isNotEmpty) ?: return null

    parseInstant(value)?.let { return it.toEpochMilli() }

    val localDateTime = sequenceOf(
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        exifDateTimeFormatter,
        dashedDateTimeFormatter,
        compactVideoMillisFormatter,
        compactVideoDateTimeFormatter
    ).firstNotNullOfOrNull { formatter ->
        try {
            LocalDateTime.parse(value.removeSuffix("Z"), formatter)
        } catch (_: DateTimeParseException) {
            null
        }
    } ?: return null

    val offset = offsetValue?.trim()?.takeIf(String::isNotEmpty)?.let {
        runCatching { ZoneOffset.of(it) }.getOrNull()
    }
    return if (offset != null) {
        localDateTime.toInstant(offset).toEpochMilli()
    } else {
        localDateTime.atZone(defaultZone).toInstant().toEpochMilli()
    }
}

private fun parseInstant(value: String): Instant? {
    runCatching { Instant.parse(value) }.getOrNull()?.let { return it }
    runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()?.let { return it }
    val normalized = value.takeIf { it.endsWith("Z") }?.let {
        when {
            it.matches(Regex("\\d{8}T\\d{6}Z")) ->
                "${it.substring(0, 4)}-${it.substring(4, 6)}-${it.substring(6, 8)}T" +
                        "${it.substring(9, 11)}:${it.substring(11, 13)}:${it.substring(13, 15)}Z"

            it.matches(Regex("\\d{8}T\\d{6}\\.\\d{3}Z")) ->
                "${it.substring(0, 4)}-${it.substring(4, 6)}-${it.substring(6, 8)}T" +
                        "${it.substring(9, 11)}:${it.substring(11, 13)}:${it.substring(13)}"

            else -> null
        }
    }
    return normalized?.let { runCatching { Instant.parse(it) }.getOrNull() }
}
