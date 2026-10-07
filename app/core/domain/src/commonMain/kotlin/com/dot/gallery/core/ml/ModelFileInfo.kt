/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ml

data class ModelFileInfo(
    val name: String,
    val size: Long,
    val sha256: String,
    val verified: Boolean
)
