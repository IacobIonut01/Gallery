/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

/**
 * Tile size variants for the mosaic layout.
 *
 * TILE_2x2: 2 columns, aspect 1:1   → height = 2w. Companion: QuadTile (span 2)
 * TILE_2x1: 2 columns, aspect 2:1   → height = w.  Companion: 2 singles (span 1 each)
 * TILE_3x2: 3 columns, aspect 3:2   → height = 2w. Companion: PairTile (span 1)
 */
enum class BigTileSize(val colSpan: Int, val aspectRatio: Float) {
    TILE_2x2(2, 1f),
    TILE_2x1(2, 2f),
    TILE_3x2(3, 1.5f),
}
