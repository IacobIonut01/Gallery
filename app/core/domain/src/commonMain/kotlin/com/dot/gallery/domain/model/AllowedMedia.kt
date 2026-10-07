/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

enum class AllowedMedia {
    PHOTOS, VIDEOS, BOTH;

    override fun toString(): String {
        return when (this) {
            PHOTOS -> "image%"
            VIDEOS -> "video%"
            BOTH -> "%/%"
        }
    }

    fun toStringAny(): String {
        return when (this) {
            PHOTOS -> "image/*"
            VIDEOS -> "video/*"
            BOTH -> "*/*"
        }
    }
}
