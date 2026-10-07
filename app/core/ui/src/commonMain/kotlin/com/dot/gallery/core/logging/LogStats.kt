/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.logging

/** Snapshot of the on-disk log store, rendered by the developer screen hero. */
data class LogStats(
    val totalBytes: Long,
    val sessionCount: Int,
    val crashCount: Int,
    val unreportedCrash: Boolean,
)
