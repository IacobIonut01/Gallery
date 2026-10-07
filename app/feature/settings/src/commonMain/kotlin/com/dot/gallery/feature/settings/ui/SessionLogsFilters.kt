/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import com.dot.gallery.core.logging.LogLevel

data class SessionLogsFilters(
    val levels: Set<LogLevel> = LogLevel.entries.toSet(),
    val query: String = "",
    val range: SessionLogsRange = SessionLogsRange.ALL,
    val rangeStart: Long? = null,
    val rangeEnd: Long? = null,
)
