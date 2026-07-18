/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.edit.adjustments

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature_node.domain.model.editor.ImageFilter
import com.dot.gallery.feature_node.domain.model.editor.TileBehavior
import com.dot.gallery.feature_node.presentation.util.applyColorMatrix

/**
 * A colour-matrix operation with an already-resolved 4x5 [matrix]. Used to record the *exact*
 * effect baked into the interactive proxy — e.g. an [ImageFilter] committed at partial intensity —
 * so the full-resolution bake reproduces it identically instead of re-applying the filter at full
 * strength. Carries the source filter's [name] so dedup/removal-by-kind keep working.
 */
class MatrixAdjustment(
    val matrix: FloatArray,
    override val name: String,
) : ImageFilter {

    override fun apply(bitmap: Bitmap): Bitmap = applyColorMatrix(bitmap, matrix)

    override fun colorMatrix(): ColorMatrix = ColorMatrix(matrix.copyOf())

    override val tileBehavior: TileBehavior get() = TileBehavior.PerPixel

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MatrixAdjustment) return false
        return name == other.name && matrix.contentEquals(other.matrix)
    }

    override fun hashCode(): Int = 31 * name.hashCode() + matrix.contentHashCode()
}
