/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import kotlinx.serialization.Serializable

@Serializable
data class CloudStorageInfo(
    val usedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val usedPercentage: Double = 0.0,
    val usedFormatted: String = "",
    val totalFormatted: String = ""
)
