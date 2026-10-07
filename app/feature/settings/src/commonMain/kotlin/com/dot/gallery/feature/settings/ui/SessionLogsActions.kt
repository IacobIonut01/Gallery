/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import com.dot.gallery.core.logging.LogEntry
import com.dot.gallery.core.logging.LogLevel

data class SessionLogsActions(
    val onModeChange: (SessionLogsMode) -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onToggleLevel: (LogLevel) -> Unit = {},
    val onRange: (SessionLogsRange) -> Unit = {},
    val onCustomRange: (startMillis: Long, endMillis: Long) -> Unit = { _, _ -> },
    val onToggleGroup: (String) -> Unit = {},
    /** Platform side-effects (clipboard + toast) live in the wrapper. */
    val onCopyEntry: (LogEntry) -> Unit = {},
    /** Platform side-effects (clipboard + toast + issue URL) live in the wrapper. */
    val onReportEntry: (LogEntry) -> Unit = {},
)
