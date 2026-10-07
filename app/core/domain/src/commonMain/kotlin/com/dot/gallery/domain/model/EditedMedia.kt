/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.dot.gallery.domain.uri.Uri
import com.dot.gallery.domain.uri.UriSerializer
import kotlinx.serialization.Serializable

@Entity(tableName = "edited_media")
data class EditedMedia(
    @PrimaryKey
    val mediaId: Long,
    @Serializable(with = UriSerializer::class)
    val originalUri: Uri,
    val backupPath: String,
    val originalMimeType: String,
    val editTimestamp: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
)
