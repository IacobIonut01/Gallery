/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.owncloud

import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.ThumbnailSize
import com.dot.gallery.cloud.webdav.OcsWebDavDialect
import com.dot.gallery.cloud.webdav.WebDavSession
import com.dot.gallery.core.util.or


/**
 * ownCloud server dialect. Inherits the OCS share/server-info behavior;
 * ownCloud does not expose favorites or quota in this integration.
 */
class OwnCloudDialect : OcsWebDavDialect() {
    override val providerType = ProviderType.OWNCLOUD
    override val displayName = "ownCloud"
    override val productName = "ownCloud"

    /**
     * Unlike Nextcloud, ownCloud's `/index.php/core/preview` endpoint rejects HTTP
     * Basic auth and 302-redirects to `/login` (it needs a browser session), and its
     * `apps/files/api/v1/thumbnail` endpoint square-crops to the requested box — an
     * aspect-ratio mismatch that makes ZoomImage discard the full-resolution
     * subsampling tiles, leaving zoomed images stuck at thumbnail resolution.
     *
     * ownCloud's SabreDAV files collection instead serves an aspect-preserving preview
     * directly from the download URL via the `?preview=1&x&y&a=1` query, which honors
     * Basic auth. Use it for images; videos have no dav preview (404), so return null
     * and let the provider fall back to a locally-decoded poster frame.
     */
    override fun previewUrl(
        session: WebDavSession,
        fileId: String,
        remotePath: String,
        size: ThumbnailSize
    ): String? {
        if (isVideoPath(remotePath)) return null
        val dim = previewDim(size)
        val downloadUrl = session.webDavClient.getDownloadUrl(remotePath)
        if (downloadUrl.isBlank()) return null
        return "$downloadUrl?preview=1&x=$dim&y=$dim&a=1"
    }
}
