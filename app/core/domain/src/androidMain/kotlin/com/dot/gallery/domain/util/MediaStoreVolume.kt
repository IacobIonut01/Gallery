package com.dot.gallery.domain.util

import android.os.Environment
import android.provider.MediaStore
import com.dot.gallery.domain.model.Media

/**
 * Resolves a destination path (absolute or relative) into a pair of
 * (MediaStore volume name, relative path).
 *
 * - Absolute paths like `/storage/emulated/0/DCIM/Camera/` resolve to
 *   `(VOLUME_EXTERNAL_PRIMARY, "DCIM/Camera/")`
 * - The bare primary storage root `/storage/emulated/0` (the "/" pseudo-bucket)
 *   resolves to `(VOLUME_EXTERNAL_PRIMARY, "/")`
 * - SD card paths like `/storage/71F8-2C0A/DCIM/Camera/` resolve to
 *   `("71f8-2c0a", "DCIM/Camera/")`
 * - Relative paths like `DCIM/Camera/` resolve to
 *   `(VOLUME_EXTERNAL_PRIMARY, "DCIM/Camera/")`
 */
fun resolveMediaStoreVolume(path: String): Pair<String, String> {
    val primaryStorage = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')

    return when {
        path.trimEnd('/') == primaryStorage -> {
            // The storage root is a valid move/copy destination (MediaStore stores
            // RELATIVE_PATH="/" for it), but parsing it as a generic
            // /storage/<id>/<rest> path would split the "emulated/0" specifier into
            // a bogus ("emulated", "0") volume.
            MediaStore.VOLUME_EXTERNAL_PRIMARY to "/"
        }

        path.startsWith("$primaryStorage/") -> {
            val relativePath = path.removePrefix("$primaryStorage/")
            MediaStore.VOLUME_EXTERNAL_PRIMARY to relativePath
        }

        path.startsWith("/storage/") -> {
            val afterStorage = path.removePrefix("/storage/")
            val volumeId = afterStorage.substringBefore("/").lowercase()
            // A bare "/storage/<id>" root has no sub-path; MediaStore expects "/"
            // for it (an empty RELATIVE_PATH is ignored as a no-op on update).
            val relativePath = afterStorage.substringAfter("/", "")
                .ifEmpty { "/" }
            volumeId to relativePath
        }

        else -> {
            MediaStore.VOLUME_EXTERNAL_PRIMARY to path
        }
    }
}

val Media.mediaStoreVolumeName: String
    get() = runCatching {
        getUri().takeIf { it.scheme == "content" && it.authority == "media" }
            ?.pathSegments?.firstOrNull()
    }.getOrNull() ?: resolveMediaStoreVolume(path).first
