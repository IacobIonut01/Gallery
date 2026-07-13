/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.help.data

import androidx.compose.runtime.Immutable

/**
 * A single app release, authored as an English-only markdown file under
 * `assets/changelog/<versionCode>.md` and loaded by [ChangelogRepository].
 */
@Immutable
data class ReleaseNotes(
    val versionName: String,
    val versionCode: Int,
    val releaseDate: String,
    /** Raw markdown body (front-matter stripped). Rendered by MarkdownText. */
    val markdown: String,
    /** Optional tip ids to surface as deep-link chips under the release. */
    val tipIds: List<String> = emptyList()
)

/**
 * Plain-text keywords for the unified help search index: strips markdown
 * heading/bullet/emphasis markers so headings and feature names remain matchable.
 */
fun ReleaseNotes.searchKeywords(): String =
    markdown.lineSequence()
        .map { it.trim().trimStart('#', '-', ' ').replace("**", "") }
        .filter { it.isNotBlank() }
        .joinToString(" ")
