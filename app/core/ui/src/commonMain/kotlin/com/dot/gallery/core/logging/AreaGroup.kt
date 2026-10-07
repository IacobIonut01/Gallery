/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.logging


/**
 * A node in the By-area tree. [id] is the raw grouping key — a screen route
 * name (`LibraryScreen`…), a scope (`worker.upload`…), or a synthetic bucket.
 * The UI maps ids to localized labels and icons.
 */
data class AreaGroup(
    val id: String,
    val kind: Kind,
    val entries: List<LogEntry>,
) {
    enum class Kind { SCREEN, SCOPE, BACKGROUND, UNGROUPED }

    val errorCount: Int get() = entries.count { it.logLevel == LogLevel.ERROR || it.logLevel == LogLevel.CRASH }
    val warnCount: Int get() = entries.count { it.logLevel == LogLevel.WARN }
    val lastTs: Long get() = entries.maxOfOrNull { it.ts } ?: 0L
}
