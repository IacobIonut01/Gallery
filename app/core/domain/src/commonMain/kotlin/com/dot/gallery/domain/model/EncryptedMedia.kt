/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import com.dot.gallery.domain.parcel.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@SerialName("com.dot.gallery.feature_node.domain.model.EncryptedMedia")
data class EncryptedMedia(
    override val id: Long = 0,
    override val label: String,
    val bytes: ByteArray,
    override val path: String,
    override val relativePath: String,
    override val albumID: Long,
    override val albumLabel: String,
    override val timestamp: Long,
    override val expiryTimestamp: Long? = null,
    override val takenTimestamp: Long? = null,
    override val fullDate: String,
    override val mimeType: String,
    override val favorite: Int,
    override val trashed: Int,
    override val size: Long,
    override val duration: String? = null
) : Media() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedMedia) return false

        if (id != other.id) return false
        if (label != other.label) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (path != other.path) return false
        if (relativePath != other.relativePath) return false
        if (albumID != other.albumID) return false
        if (albumLabel != other.albumLabel) return false
        if (timestamp != other.timestamp) return false
        if (expiryTimestamp != other.expiryTimestamp) return false
        if (takenTimestamp != other.takenTimestamp) return false
        if (fullDate != other.fullDate) return false
        if (mimeType != other.mimeType) return false
        if (favorite != other.favorite) return false
        if (trashed != other.trashed) return false
        if (size != other.size) return false
        if (duration != other.duration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + relativePath.hashCode()
        result = 31 * result + albumID.hashCode()
        result = 31 * result + albumLabel.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + (expiryTimestamp?.hashCode() ?: 0)
        result = 31 * result + (takenTimestamp?.hashCode() ?: 0)
        result = 31 * result + fullDate.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + favorite
        result = 31 * result + trashed
        result = 31 * result + size.hashCode()
        result = 31 * result + (duration?.hashCode() ?: 0)
        return result
    }
}
