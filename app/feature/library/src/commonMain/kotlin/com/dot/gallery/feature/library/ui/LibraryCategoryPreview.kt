/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.library.ui

import com.dot.gallery.domain.model.UriMedia
import kotlinx.serialization.Serializable


@Serializable
data class LibraryCategoryPreview(
    val id: Long,
    val name: String,
    val mediaCount: Int,
    val thumbnailMedia: UriMedia? = null
)
