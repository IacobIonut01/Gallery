/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import com.dot.gallery.domain.date.formatEpochSec
import com.dot.gallery.domain.date.parseTimestampFromFilename
import com.dot.gallery.domain.model.UriMedia
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.datetime.TimeZone

/**
 * Provider-reported capture time, else the filename-derived one — the same
 * resolution [toUriMedia] applies. Shared with [CloudMediaSnapshotMapper] so
 * the pre-computed display date matches what the mapped media ends up with.
 */
internal fun CloudMediaEntity.effectiveTakenTimestamp(): Long? =
    takenTimestamp ?: label.parseTimestampFromFilename()

class CloudMediaSnapshotMapper {
    private data class Entry(val source: CloudMediaEntity, val media: UriMedia)
    private var entries = emptyMap<Long, Entry>()
    private var formatContext: String? = null

    fun map(source: List<CloudMediaEntity>): List<UriMedia> {
        // One probe call captures the whole formatting context (system time
        // zone + locale), so a change on either axis invalidates cached dates.
        val currentFormat = "${TimeZone.currentSystemDefault().id}|" +
            0L.formatEpochSec(EXTENDED_DATE_FORMAT)
        val sameFormatting = formatContext == currentFormat
        val next = HashMap<Long, Entry>(source.size)
        val result = ArrayList<UriMedia>(source.size)
        for (entity in source) {
            val previous = entries[entity.globalMediaId]
            val entry = if (sameFormatting && previous != null &&
                previous.source.hasSameMediaPresentation(entity)
            ) previous else {
                val seconds = entity.effectiveTakenTimestamp()?.let { it / 1000L }
                    ?: (entity.timestamp / 1000L)
                val date = seconds.formatEpochSec(EXTENDED_DATE_FORMAT)
                Entry(entity, entity.toUriMedia(fullDate = date))
            }
            next[entity.globalMediaId] = entry
            result += entry.media
        }
        entries = next
        formatContext = currentFormat
        return result
    }
}

private fun CloudMediaEntity.hasSameMediaPresentation(other: CloudMediaEntity): Boolean =
    globalMediaId == other.globalMediaId && providerType == other.providerType &&
    serverConfigId == other.serverConfigId && remoteId == other.remoteId && fileId == other.fileId &&
    label == other.label && path == other.path && relativePath == other.relativePath &&
    mimeType == other.mimeType && timestamp == other.timestamp && takenTimestamp == other.takenTimestamp &&
    size == other.size && duration == other.duration && favorite == other.favorite && trashed == other.trashed

fun CloudMediaEntity.backupFingerprint(): String =
    contentHash?.takeIf { it.isNotBlank() }?.let(::canonicalBackupChecksum) ?: "$size|$timestamp"

@OptIn(ExperimentalEncodingApi::class)
fun canonicalBackupChecksum(value: String): String {
    val normalized = value.lowercase()
    if (normalized.length == 40 && normalized.all { it in '0'..'9' || it in 'a'..'f' }) {
        return normalized
    }
    return runCatching { Base64.decode(value) }
        .getOrNull()
        ?.takeIf { it.size == 20 }
        ?.joinToString("") { byte ->
            val b = byte.toInt() and 0xFF
            HEX_DIGITS[b shr 4].toString() + HEX_DIGITS[b and 0xF]
        }
        ?: value
}

private const val EXTENDED_DATE_FORMAT = "EEE, MMM d, yyyy"
private const val HEX_DIGITS = "0123456789abcdef"
