/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.help.domain

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource

@Immutable
data class TutorialPage(
    val title: StringResource,
    val description: StringResource,
    val steps: List<StringResource> = emptyList(),
    val actionLabel: StringResource? = null,
    val actionRoute: HelpRoute? = null,
    val previewType: PreviewType = PreviewType.NONE,
    /**
     * Ordered preview frames for multi-step flows. When non-empty the detail
     * screen renders a stepper that walks through each frame; otherwise the
     * single [previewType] is used.
     */
    val previewSteps: List<PreviewType> = emptyList(),
    /** Optional per-step captions, index-aligned with [previewSteps]. */
    val stepCaptions: List<StringResource> = emptyList()
)
