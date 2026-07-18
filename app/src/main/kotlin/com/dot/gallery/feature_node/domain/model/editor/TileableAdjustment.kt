/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.model.editor

import android.graphics.Bitmap

/**
 * An [Adjustment] whose result for a sub-region (tile) depends on the tile's position within the
 * full image — e.g. an analytic radial vignette (centre + radius derived from the full image) or a
 * markup overlay stretched across the whole image. The memory-bounded tiled bake calls [applyTile]
 * with the full-image geometry so each tile is rendered identically to the whole-image
 * [Adjustment.apply], letting the engine process the image in strips without ever materialising the
 * full-resolution source bitmap.
 *
 * Contract: `applyTile(region, W, H, x, y)` MUST be pixel-identical to cropping the rectangle
 * `[x, x+region.width) x [y, y+region.height)` out of `apply(fullImage)` where `fullImage` is
 * `W x H`. Ops that are position-independent (per-pixel colour, local kernels) do NOT implement
 * this — the engine applies them directly to the tile.
 */
interface TileableAdjustment : Adjustment {

    fun applyTile(
        tile: Bitmap,
        fullWidth: Int,
        fullHeight: Int,
        tileX: Int,
        tileY: Int,
    ): Bitmap
}
