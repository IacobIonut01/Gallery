/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import com.dot.gallery.data.entity.CloudServerConfigEntity
import com.dot.gallery.data.entity.CloudUploadPrefEntity
import com.dot.gallery.domain.model.Media
import com.dot.gallery.core.util.or
import com.dot.gallery.domain.util.isVideo


/**
 * Single source of truth for the remote folder a local media item is uploaded to
 * on path-based providers (WebDAV/ownCloud/Nextcloud/SMB/NFS).
 *
 * Resolution order:
 * 1. [CloudUploadPrefEntity.customPath] — a per-album override used verbatim.
 * 2. The account base — [CloudServerConfigEntity.uploadVideosPath] for videos
 *    when set, otherwise [CloudServerConfigEntity.uploadBasePath] — joined with
 *    the album label so each backed-up album still lands in its own subfolder.
 * 3. `null` when everything resolves blank, letting the provider fall back to
 *    its default upload folder.
 *
 * Content-addressable providers (Immich) ignore the resolved path entirely.
 */
object UploadTargetResolver {

    /**
     * Normalizes a user- or album-derived remote path: backslashes become
     * separators, duplicate separators collapse, and `.`/`..` segments are
     * dropped so a stored value can never escape the provider's files root.
     * Returns "" when nothing usable remains.
     */
    fun sanitizePath(raw: String): String =
        raw.trim()
            .replace('\\', '/')
            .split('/')
            .filter { it.isNotEmpty() && it != "." && it != ".." }
            .joinToString("/")

    fun resolve(config: CloudServerConfigEntity?, pref: CloudUploadPrefEntity, media: Media): String? =
        resolve(config, pref, isVideo = media.mimeType.startsWith("video/"))

    fun resolve(
        config: CloudServerConfigEntity?,
        pref: CloudUploadPrefEntity,
        isVideo: Boolean
    ): String? {
        val custom = sanitizePath(pref.customPath)
        if (custom.isNotEmpty()) return custom
        val base = sanitizePath(
            if (isVideo && config?.uploadVideosPath?.isNotBlank() == true) config.uploadVideosPath
            else config?.uploadBasePath.orEmpty()
        )
        val album = sanitizePath(pref.albumLabel)
        return when {
            album.isEmpty() -> base.ifEmpty { null }
            base.isEmpty() -> album
            else -> "$base/$album"
        }
    }
}
