/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import androidx.room3.Entity
import com.dot.gallery.domain.parcel.Parcelize
import com.dot.gallery.domain.uri.Uri
import com.dot.gallery.domain.uri.UriSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@SerialName("com.dot.gallery.feature_node.domain.model.ClassifiedMedia")
@Entity(tableName = "classified_media", primaryKeys = ["id"])
data class ClassifiedMedia(
    override val id: Long = 0,
    override val label: String,
    @Serializable(with = UriSerializer::class)
    val uri: Uri,
    override val path: String,
    override val relativePath: String,
    override val albumID: Long,
    override val albumLabel: String,
    override val timestamp: Long,
    override val expiryTimestamp: Long?,
    override val takenTimestamp: Long?,
    override val fullDate: String,
    override val mimeType: String,
    override val favorite: Int,
    override val trashed: Int,
    override val size: Long,
    override val duration: String?,
    val category: String?,
    val score: Float,
) : Media()
