/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.help.data

import android.content.Context
import com.dot.gallery.feature.help.domain.ChangelogParser
import com.dot.gallery.feature.help.domain.ReleaseNotes

/**
 * Loads the changelog from English-only markdown files bundled under the
 * `assets/changelog` directory (`<versionCode>.md`). The file name (its integer
 * version code) defines ordering; a small YAML-ish front-matter block carries
 * the display version, date and optional linked tip ids — parsed by the
 * multiplatform [ChangelogParser].
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
                ChangelogParser.parse(code, text)
            }.getOrNull()
        }.sortedByDescending { it.versionCode }
        cache = releases
        return releases
    }

    fun getCurrent(context: Context): ReleaseNotes? = getAll(context).firstOrNull()
}
