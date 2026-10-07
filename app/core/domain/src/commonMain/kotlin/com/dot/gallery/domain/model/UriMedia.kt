/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import com.dot.gallery.domain.parcel.Parcelize
import com.dot.gallery.domain.uri.Uri
import com.dot.gallery.domain.uri.UriSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// SerialName preserves the pre-KMP nested FQN persisted by the Room
// `Converters.fromMedia` JSON column — existing rows keep decoding.
@Serializable
@Parcelize
@SerialName("com.dot.gallery.feature_node.domain.model.UriMedia")
@Entity(tableName = "media", primaryKeys = ["id"])
data class UriMedia(
    override val id: Long = 0,
    override val label: String,
    @Serializable(with = UriSerializer::class)
    val uri: Uri,
    override val path: String,
    override val relativePath: String,
    override val albumID: Long,
    override val albumLabel: String,
    override val timestamp: Long,
    override val expiryTimestamp: Long? = null,
    override val takenTimestamp: Long? = null,
    @ColumnInfo(defaultValue = "'modified_fallback'")
    val captureTimeOrigin: CaptureTimeOrigin = if (takenTimestamp != null) {
        CaptureTimeOrigin.MEDIA_STORE
    } else {
        CaptureTimeOrigin.MODIFIED_FALLBACK
    },
    override val fullDate: String,
    override val mimeType: String,
    override val favorite: Int,
    override val trashed: Int,
    override val size: Long,
    override val duration: String? = null
) : Media()
