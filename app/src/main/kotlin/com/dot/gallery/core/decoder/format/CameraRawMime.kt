/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder.format

/**
 * MIME registry for camera RAW formats. Most vendor RAW files are reported as `image/x-*` or
 * `image/vnd.*` by MediaStore, but a few non-RAW formats share those prefixes.
 */
object CameraRawMime {

    private val EXCLUDED_MIME_TYPES = setOf(
        "image/vnd.adobe.photoshop",
        "image/vnd.ms-photo",
        "image/vnd.wap.wbmp",
        "image/x-photoshop",
        "image/photoshop",
    )

    fun isCameraRaw(mime: String?): Boolean {
        if (mime.isNullOrEmpty()) return false
        val lower = mime.lowercase().substringBefore(';').trim()
        if (lower in EXCLUDED_MIME_TYPES) return false
        return lower.startsWith("image/x-") || lower.startsWith("image/vnd.")
    }

    fun isDng(mime: String?): Boolean = mime?.lowercase()?.contains("dng") == true
}
