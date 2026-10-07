/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

/**
 * Dynamically computed mosaic tile pattern for a given column count.
 *
 * Each pattern defines how a "block" fills exactly [totalColumns] columns:
 *  - A big tile taking [bigTileSpan] columns
 *  - A companion type filling the remaining columns
 *
 * Heights match because aspect ratios are coordinated:
 *  - QUAD: big aspect = bigSpan/quadSpan, quad container is 1:1 (2×2 grid)
 *  - PAIR: big aspect = bigSpan/2, pair is 0.5 (two stacked squares)
 *  - SINGLES: big aspect = bigSpan (one row tall), singles are 1:1
 */
data class MosaicTilePattern(
    val bigTileSpan: Int,
    val bigTileAspectRatio: Float,
    val companionType: CompanionType,
    val itemsConsumed: Int,
) {
    enum class CompanionType { QUAD, PAIR, SINGLES }
}

fun mosaicPatternsForColumns(columns: Int): List<MosaicTilePattern> {
    val patterns = mutableListOf<MosaicTilePattern>()

    // QUAD pattern: big(N-2) + quad(2). Needs columns >= 4, consumes 5 items.
    // big aspect = (N-2) / 2
    if (columns >= 4) {
        val bigSpan = columns - 2
        val bigAspect = bigSpan.toFloat() / 2f
        if (bigAspect <= 2.5f) {
            patterns.add(
                MosaicTilePattern(
                    bigTileSpan = bigSpan,
                    bigTileAspectRatio = bigAspect,
                    companionType = MosaicTilePattern.CompanionType.QUAD,
                    itemsConsumed = 5
                )
            )
        }
    }

    // PAIR pattern: big(N-1) + pair(1). Needs columns >= 3, consumes 3 items.
    // big aspect = (N-1) / 2
    if (columns >= 3) {
        val bigSpan = columns - 1
        val bigAspect = bigSpan.toFloat() / 2f
        if (bigAspect <= 2.5f) {
            patterns.add(
                MosaicTilePattern(
                    bigTileSpan = bigSpan,
                    bigTileAspectRatio = bigAspect,
                    companionType = MosaicTilePattern.CompanionType.PAIR,
                    itemsConsumed = 3
                )
            )
        }
    }

    // SINGLES pattern: big(2) + (N-2) singles. Needs columns >= 3, consumes 1+(N-2)=N-1 items.
    // big aspect = 2.0 (2 cols wide, 1 row)
    if (columns >= 3) {
        patterns.add(
            MosaicTilePattern(
                bigTileSpan = 2,
                bigTileAspectRatio = 2f,
                companionType = MosaicTilePattern.CompanionType.SINGLES,
                itemsConsumed = columns - 1
            )
        )
    }

    return patterns
}
