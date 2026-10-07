/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import com.dot.gallery.core.logging.LogEntry
import com.dot.gallery.core.logging.LogLevel
import com.dot.gallery.core.logging.LogStats

data class DeveloperScreenState(
    val stats: LogStats = LogStats(0, 0, 0, false),
    val totalCount: Int = 0,
    val latestErrors: List<LogEntry> = emptyList(),
    val entries: List<LogEntry> = emptyList(),
    val unreportedCrashEntry: LogEntry? = null,
    val minLevel: LogLevel = LogLevel.INFO,
    val breadcrumbsEnabled: Boolean = false,
)
