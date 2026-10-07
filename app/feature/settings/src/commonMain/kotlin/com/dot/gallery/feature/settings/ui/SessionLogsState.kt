/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import com.dot.gallery.core.logging.AreaGroup
import com.dot.gallery.core.logging.LogEntry

data class SessionLogsState(
    val loading: Boolean = true,
    val mode: SessionLogsMode = SessionLogsMode.RECENT,
    val filters: SessionLogsFilters = SessionLogsFilters(),
    val entries: List<LogEntry> = emptyList(),
    val groups: List<AreaGroup> = emptyList(),
    val expandedGroups: Set<String> = emptySet(),
)
