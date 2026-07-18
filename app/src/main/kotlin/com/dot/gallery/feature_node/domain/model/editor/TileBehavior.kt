/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.model.editor

/**
 * Describes how an [Adjustment] can be re-applied to the full-resolution original in memory-bounded
 * tiles by the bake engine (see `core/editor/bake`). Interactive editing always happens on a
 * downscaled proxy; the [TileBehavior] tells the bake engine how much source context each op needs
 * so the exact same visual result is reproduced at full resolution without ever holding the whole
 * image in memory.
 */
sealed interface TileBehavior {

    /**
     * Pure per-pixel transform (colour matrices, posterize, negative, …). A tile can be processed in
     * isolation with no neighbouring context.
     */
    data object PerPixel : TileBehavior

    /**
     * Neighbourhood/convolution op (sharpen, denoise, edge-detect, blur brushes). Each output tile
     * needs [radius] pixels of halo decoded around it; the halo is processed then cropped away so
     * tile seams match a full-frame pass.
     */
    data class Kernel(val radius: Int) : TileBehavior

    /**
     * Depends on the full image geometry (vignette gradient, solid borders). The op is recomputed
     * per tile from the global dimensions/origin rather than from local pixels alone.
     */
    data object Analytic : TileBehavior

    /**
     * Changes image geometry/dimensions (crop, 90° rotate, flip, arbitrary rotate, border expand).
     * The bake engine maps each output tile back through the inverse transform to the source region
     * it samples from.
     */
    data object Geometry : TileBehavior

    /**
     * Vector markup (paths, text, face-blur/mosaic regions) rendered at arbitrary scale. Solid
     * strokes/text render exactly at any tile size; blur/mosaic brush regions carry their own halo
     * handling during the bake.
     */
    data object Markup : TileBehavior
}
