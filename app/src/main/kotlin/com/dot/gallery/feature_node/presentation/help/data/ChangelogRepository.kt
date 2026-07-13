/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.help.data

import android.content.Context

/**
 * Loads the changelog from English-only markdown files bundled under
 * `assets/changelog/<versionCode>.md`. The file name (its integer version code)
 * defines ordering; a small YAML-ish front-matter block carries the display
 * version, date and optional linked tip ids:
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
 * Results are cached after the first read; the asset set is immutable at runtime.
 */
object ChangelogRepository {

    private const val DIR = "changelog"

    @Volatile
    private var cache: List<ReleaseNotes>? = null

    /** All releases, newest (highest version code) first. */
    fun getAll(context: Context): List<ReleaseNotes> {
        cache?.let { return it }
        val assets = context.applicationContext.assets
        val files = runCatching { assets.list(DIR)?.toList() }.getOrNull().orEmpty()
            .filter { it.endsWith(".md") }
        val releases = files.mapNotNull { name ->
            val code = name.removeSuffix(".md").toIntOrNull() ?: return@mapNotNull null
            runCatching {
                val text = assets.open("$DIR/$name").bufferedReader().use { it.readText() }
                parse(code, text)
            }.getOrNull()
        }.sortedByDescending { it.versionCode }
        cache = releases
        return releases
    }

    fun getCurrent(context: Context): ReleaseNotes? = getAll(context).firstOrNull()

    private fun parse(versionCode: Int, raw: String): ReleaseNotes {
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
