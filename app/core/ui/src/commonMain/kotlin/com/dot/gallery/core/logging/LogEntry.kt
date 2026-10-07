/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.logging

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One sanitized diagnostics line. Entries are written as JSON Lines
 * (`session-*.jsonl`) and rendered verbatim by the developer screen, so every
 * field must already be anonymized before construction.
 *
 * [screen] is the visible screen at write time (stamped by `AppLog` from
 * navigation). [scope] is the feature domain (`cloud.immich`, `worker.upload`,
 * `decode.heic`, `media-ops`…) and survives off-screen work. Together they feed
 * the By-area grouping.
 */
@Serializable
data class LogEntry(
    val ts: Long,
    val level: String,
    val tag: String,
    val message: String,
    val ctx: Map<String, String>? = null,
    val stackTrace: String? = null,
    val session: String? = null,
    val screen: String? = null,
    val scope: String? = null,
    val crumbs: List<Breadcrumb>? = null,
) {

    val logLevel: LogLevel
        get() = runCatching { LogLevel.valueOf(level) }.getOrDefault(LogLevel.INFO)

    val isError: Boolean
        get() = logLevel == LogLevel.ERROR || logLevel == LogLevel.CRASH

    fun toJson(json: Json): String = json.encodeToString(serializer(), this)

    companion object {
        fun fromJson(json: Json, line: String): LogEntry? =
            runCatching { json.decodeFromString<LogEntry>(line) }.getOrNull()
    }
}
