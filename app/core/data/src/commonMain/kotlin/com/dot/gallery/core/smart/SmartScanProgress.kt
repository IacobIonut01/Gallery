/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.smart

data class SmartScanProgress(
    val total: Int,
    val processed: Int,
    val succeeded: Int,
    val skipped: Int,
    val failed: Int
) {
    init {
        require(total >= 0 && processed in 0..total)
        require(succeeded >= 0 && skipped >= 0 && failed >= 0)
        require(succeeded + skipped + failed <= processed)
    }

    val percent: Int
        get() = if (total <= 0) 0 else ((processed.coerceAtMost(total) * 100L) / total).toInt()

    operator fun plus(other: SmartScanProgress) = SmartScanProgress(
        total = total + other.total,
        processed = processed + other.processed,
        succeeded = succeeded + other.succeeded,
        skipped = skipped + other.skipped,
        failed = failed + other.failed
    )

    companion object {
        val EMPTY = SmartScanProgress(0, 0, 0, 0, 0)
    }
}
