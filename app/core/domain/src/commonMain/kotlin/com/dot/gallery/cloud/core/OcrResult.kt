/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import kotlinx.serialization.Serializable

@Serializable
data class OcrResult(
    val fullText: String,
    val blocks: List<OcrBlock> = emptyList(),
    val providerType: ProviderType
)
