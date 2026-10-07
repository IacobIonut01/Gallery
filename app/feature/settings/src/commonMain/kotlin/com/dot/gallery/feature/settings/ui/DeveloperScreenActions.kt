/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import com.dot.gallery.core.logging.LogEntry
import com.dot.gallery.core.logging.LogLevel

/**
 * Every platform/VM side-effect of the developer screen: clipboard, Toast,
 * share intents and navigation are owned by the wrapper.
 */
data class DeveloperScreenActions(
    val onOpenSessionLogs: () -> Unit = {},
    /** Clipboard + toast + issue URL; `null` reports the current filtered log. */
    val onReportBug: (LogEntry?) -> Unit = {},
    val onShareZip: (List<LogEntry>) -> Unit = {},
    val onCopyEntries: (List<LogEntry>) -> Unit = {},
    val onCopyEntry: (LogEntry) -> Unit = {},
    val onClearLogs: () -> Unit = {},
    /** Wrapper also flips the developer-mode pref and navigates up. */
    val onDisableDeveloperMode: () -> Unit = {},
    val onToggleBreadcrumbs: (Boolean) -> Unit = {},
    val onSetMinLevel: (LogLevel) -> Unit = {},
    val onTestError: () -> Unit = {},
    val onMarkCrashReported: () -> Unit = {},
)
