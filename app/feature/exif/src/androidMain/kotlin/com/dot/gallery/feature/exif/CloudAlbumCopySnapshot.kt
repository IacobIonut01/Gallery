/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.exif

import com.dot.gallery.domain.model.Media
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Decoded snapshot of a `CloudAlbumCopyWorker` work item — null fields mean the
 * key was absent from the work data (progress vs terminal payload).
 */
data class CloudAlbumCopySnapshot(
    val state: CopyWorkState,
    val requestId: String? = null,
    val mode: String? = null,
    val destinationProvider: String? = null,
    val destinationConfigId: Long? = null,
    val destinationRemoteAlbumId: String? = null,
    val destinationAlbumId: Long? = null,
    val destinationLabel: String? = null,
    val targetRemoteId: String? = null,
    val total: Int? = null,
    val completed: Int? = null,
    val alreadyPresent: Int? = null,
    val failed: Int? = null,
    val current: Int? = null,
    val retryable: Boolean = false,
    val message: String? = null,
)