/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.exif.ui

import com.dot.gallery.core.metadata.MetadataDirectory

data class MetadataViewState(
    val isLoading: Boolean = true,
    val directories: List<MetadataDirectory> = emptyList()
)
