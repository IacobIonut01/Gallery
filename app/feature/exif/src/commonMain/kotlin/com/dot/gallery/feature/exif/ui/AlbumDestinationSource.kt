/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.exif.ui

/** (volume, relativePath) pair describing one source folder of a move selection. */
data class AlbumDestinationSource(
    val volume: String,
    val relativePath: String,
)
