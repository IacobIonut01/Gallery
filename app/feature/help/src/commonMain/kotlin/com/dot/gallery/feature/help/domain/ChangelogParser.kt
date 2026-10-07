/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.help.domain

/**
 * Parser for the changelog markdown files bundled under
 * `assets/changelog/<versionCode>.md`. The file name (its integer version
 * code) defines ordering; a small YAML-ish front-matter block carries the
 * display version, date and optional linked tip ids:
 *
 * ```
 * ---
 * version: 5.1.0
 * date: 2026-07-13
 * tips: tip_a, tip_b
 * ---
 * ## What's new in 5.1.0
 * ...markdown body...
 * ```
 *
 * Pure Kotlin — the platform side supplies the raw file text.
 */
object ChangelogParser {

    fun parse(versionCode: Int, raw: String): ReleaseNotes {
        var version = ""
        var date = ""
        var tips = emptyList<String>()
        var body = raw.trim()

        if (body.startsWith("---")) {
            val end = body.indexOf("\n---", 3)
            if (end > 0) {
                val front = body.substring(3, end).trim()
                body = body.substring(end + 4).trim()
                front.lineSequence().forEach { line ->
                    val idx = line.indexOf(':')
                    if (idx > 0) {
                        val key = line.substring(0, idx).trim().lowercase()
                        val value = line.substring(idx + 1).trim()
                        when (key) {
                            "version" -> version = value
                            "date" -> date = value
                            "tips" -> tips = value.split(',')
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                        }
                    }
                }
            }
        }

        return ReleaseNotes(
            versionName = version.ifBlank { versionCode.toString() },
            versionCode = versionCode,
            releaseDate = date,
            markdown = body,
            tipIds = tips
        )
    }
}
