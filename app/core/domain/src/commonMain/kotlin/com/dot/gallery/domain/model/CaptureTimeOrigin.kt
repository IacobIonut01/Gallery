/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class CaptureTimeOrigin(val storedValue: String) {
    EMBEDDED_IMAGE("embedded_image"),
    VIDEO_CONTAINER("video_container"),
    CLOUD_PROVIDER("cloud_provider"),
    MEDIA_STORE("media_store"),
    FILENAME("filename"),
    MODIFIED_FALLBACK("modified_fallback");

    companion object {
        fun fromStoredValue(value: String): CaptureTimeOrigin =
            entries.firstOrNull { it.storedValue == value } ?: MODIFIED_FALLBACK
    }
}
