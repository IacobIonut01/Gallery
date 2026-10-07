/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs

import androidx.compose.runtime.Composable

/**
 * One renderable docs scene: a composable plus the metadata the site needs to
 * embed it (stable id for URLs/iframes, human title, and the in-app path trace
 * shown next to the live render).
 */
data class DocsScene(
    val id: String,
    val title: String,
    val pathTrace: List<String> = emptyList(),
    val states: List<String> = emptyList(),
    val content: @Composable (state: String?) -> Unit,
)
