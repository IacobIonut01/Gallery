/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.startup

import android.os.Build
import java.net.URI
import java.net.URISyntaxException

const val PERMISSION_MASK_LEGACY_STORAGE = 1
const val PERMISSION_MASK_FULL_MEDIA = 3

/**
 * Coarse permission mask stored alongside cached snapshots so a permission grant/revoke
 * invalidates the cache. Null on SDK < R (no partial-media permission model).
 */
fun fullReadPermissionMask(
    sdkInt: Int,
    imagesGranted: Boolean,
    videosGranted: Boolean,
    legacyGranted: Boolean
): Int? = when {
    sdkInt < Build.VERSION_CODES.R -> null
    sdkInt >= Build.VERSION_CODES.TIRAMISU ->
        if (imagesGranted && videosGranted) PERMISSION_MASK_FULL_MEDIA else null
    else -> if (legacyGranted) PERMISSION_MASK_LEGACY_STORAGE else null
}

/** Extracts the row id from a `content://media/...` uri, or null for any other shape. */
fun mediaStoreUriId(value: String): Long? = try {
    val uri = URI(value)
    if (uri.scheme != "content" || uri.authority != "media") null
    else uri.path.substringAfterLast('/').toLongOrNull()?.takeIf { it >= 0 }
} catch (_: URISyntaxException) {
    null
}
