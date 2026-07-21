/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.model.editor

import kotlinx.serialization.Serializable

/**
 * The RAW develop categories, each surfaced as its own top-level editor tab (White balance, Tone,
 * Detail, Colour, Output). Carried as the argument of [EditorDestination.Develop] so a single
 * destination renders the matching control group.
 */
@Serializable
enum class DevelopCategory {
    WhiteBalance,
    Tone,
    Detail,
    Colour,
    Output,
}
