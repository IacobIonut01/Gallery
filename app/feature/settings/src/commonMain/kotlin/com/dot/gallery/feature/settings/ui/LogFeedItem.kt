/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import com.dot.gallery.core.logging.LogEntry

const val CRASH_SESSION_KEY = "__crashes__"

sealed interface LogFeedItem {
    data class Header(val sessionId: String?, val key: String) : LogFeedItem
    data class Entry(val entry: LogEntry, val key: String) : LogFeedItem
}

fun buildLogFeed(entries: List<LogEntry>): List<LogFeedItem> {
    val out = ArrayList<LogFeedItem>(entries.size + 8)
    var lastSession: String? = null
    var headerSeq = 0
    entries.forEachIndexed { index, e ->
        val session = e.session ?: CRASH_SESSION_KEY
        if (session != lastSession) {
            out += LogFeedItem.Header(sessionId = e.session, key = "header-${headerSeq++}")
            lastSession = session
        }
        out += LogFeedItem.Entry(e, key = "entry-$index-${e.ts}")
    }
    return out
}
