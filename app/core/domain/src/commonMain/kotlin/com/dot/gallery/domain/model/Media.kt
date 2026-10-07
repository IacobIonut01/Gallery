/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import com.dot.gallery.domain.parcel.Parcelable
import com.dot.gallery.domain.parcel.Parcelize
import com.dot.gallery.domain.util.getUri
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
sealed class Media : Parcelable {

    abstract val id: Long
    abstract val label: String
    abstract val path: String
    abstract val relativePath: String
    abstract val albumID: Long
    abstract val albumLabel: String
    abstract val timestamp: Long
    abstract val expiryTimestamp: Long?
    abstract val takenTimestamp: Long?
    abstract val fullDate: String
    abstract val mimeType: String
    abstract val favorite: Int
    abstract val trashed: Int
    abstract val size: Long
    abstract val duration: String?

    companion object
}

val Media.definedTimestamp: Long
    get() = takenTimestamp?.div(1000) ?: timestamp

val Media.key: String
    get() = "{$id, ${
        try {
            getUri()
        } catch (_: Exception) {
            path
        }
    }, $definedTimestamp}"

val Media.idLessKey: String
    get() = "{${
        try {
            getUri()
        } catch (_: Exception) {
            path
        }
    }, $definedTimestamp}"
