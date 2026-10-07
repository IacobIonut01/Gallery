/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.help.domain

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.DrawableResource

/**
 * A tip icon: either a material [vector] or a bundled [drawable] resource.
 * The Android-only `@DrawableRes Int` variant became a compose-resources
 * [DrawableResource] for KMP.
 */
@Immutable
data class HelpIcon(
    val vector: ImageVector? = null,
    val drawable: DrawableResource? = null
) {
    companion object {
        fun ofVector(vector: ImageVector) = HelpIcon(vector = vector)
        fun ofDrawable(resource: DrawableResource) = HelpIcon(drawable = resource)
    }
}
