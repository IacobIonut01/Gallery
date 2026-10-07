/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.data.dao
data class ImageEmbeddingHeader(
    val id: Long,
    val date: Long,
    val resultRevision: String,
    val embeddingBytes: Int
)
