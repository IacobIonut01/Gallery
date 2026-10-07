/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.webdav

import android.net.Uri
import android.text.format.Formatter
import com.dot.gallery.cloud.core.CloudServerInfo
import com.dot.gallery.cloud.core.CloudStorageInfo
import com.dot.gallery.cloud.core.ThumbnailSize
import com.dot.gallery.core.util.or
import com.dot.gallery.domain.uri.encodedPath
import com.dot.gallery.domain.uri.path


/**
 * Shared base dialect for ownCloud-family servers that speak OCS (`/ocs/v2.php`)
 * and expose the `/index.php/core/preview` thumbnailer. Concrete servers
 * (ownCloud, Nextcloud) only need to set their identity and opt into extra
 * features such as favorites or quota.
 */
abstract class OcsWebDavDialect : WebDavDialect() {

    /** Product name used when composing the server identity string. */
    protected abstract val productName: String

    override val features: Set<WebDavFeatureKey> = setOf(
        WebDavFeatureKey.SERVER_PREVIEW,
        WebDavFeatureKey.SHARE_LINK,
        WebDavFeatureKey.SERVER_INFO
    )

    override fun filesEndpoint(username: String): String = "/remote.php/dav/files/$username"

    override fun previewUrl(
        session: WebDavSession,
        fileId: String,
        remotePath: String,
        size: ThumbnailSize
    ): String? {
        val dim = when (size) {
            ThumbnailSize.THUMBNAIL -> 256
            ThumbnailSize.PREVIEW -> 1024
        }
        val base = session.baseUrl.trimEnd('/')

        // Videos: the Files-app thumbnail API only renders images and returns
        // HTTP 404 for video files. The `core/preview` endpoint, by contrast,
        // generates a video frame preview (when the server has the preview
        // generator / ffmpeg) and DOES accept Basic auth when addressed by the
        // numeric `fileId` (the historical `file=` path param is what produced
        // HTTP 400). We therefore use core/preview by id for videos, and return
        // null when no usable numeric id is available so the caller can skip it.
        if (isVideoPath(remotePath)) {
            val numericId = fileId.takeIf { it.isNotBlank() && it != remotePath && it.all(Char::isDigit) }
                ?: return null
            return "$base/index.php/core/preview?fileId=$numericId&x=$dim&y=$dim&a=0&forceIcon=0&mode=cover"
        }

        // Images: prefer the `core/preview` endpoint addressed by the numeric
        // `fileId`. Unlike the Files thumbnail API (which square-crops to
        // dim*dim), core/preview with `a=1` PRESERVES the original aspect ratio.
        // That matters for the media viewer: ZoomImage only swaps in the
        // full-resolution subsampling tiles when the base preview and the
        // original share an aspect ratio, so a square-cropped preview left
        // ownCloud/Nextcloud images stuck at thumbnail resolution when zoomed
        // (the original was fetched, then its tiles discarded). core/preview by
        // fileId also accepts Basic auth — only the path-based route fails
        // (ownCloud 302-redirects to login, Nextcloud rejects the `file=` param).
        val numericId = fileId.takeIf { it.isNotBlank() && it != remotePath && it.all(Char::isDigit) }
        if (numericId != null) {
            return "$base/index.php/core/preview?fileId=$numericId&x=$dim&y=$dim&a=1&forceIcon=0&mode=fill"
        }
        // No usable numeric id: fall back to the Files thumbnail API. It
        // authenticates with Basic auth and returns a real (square, cropped)
        // image on both ownCloud 10 and Nextcloud. The path is appended as URL
        // path segments (slashes preserved, other characters percent-encoded).
        val encodedPath = Uri.encode(remotePath.trimStart('/'), "/")
        return "$base/index.php/apps/files/api/v1/thumbnail/$dim/$dim/$encodedPath"
    }

    protected fun isVideoPath(path: String): Boolean =
        path.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS

    /** Preview box dimension (px) for each [ThumbnailSize]. */
    protected fun previewDim(size: ThumbnailSize): Int = when (size) {
        ThumbnailSize.THUMBNAIL -> 256
        ThumbnailSize.PREVIEW -> 1024
    }

    protected companion object {
        val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "m4v")
    }

    override suspend fun createShareLink(
        session: WebDavSession,
        path: String,
        expiresAt: Long?
    ): Result<String> = try {
        val expDate = expiresAt?.let {
            java.time.Instant.ofEpochMilli(it)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate().toString()
        }
        Result.success(session.ocs.createPublicShare(path, expirationDate = expDate).url)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun serverVersion(session: WebDavSession): Result<String> = try {
        val caps = session.ocs.getCapabilities()
        Result.success(caps.versionString.ifBlank { caps.version })
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun serverInfo(session: WebDavSession): CloudServerInfo {
        val caps = runCatching { session.ocs.getCapabilities() }.getOrNull()
        return CloudServerInfo(
            version = caps?.version ?: "unknown",
            serverName = "$productName ${caps?.versionString ?: ""}".trim()
        )
    }

    override suspend fun currentUserId(session: WebDavSession): String? =
        runCatching { session.ocs.getCurrentUser().id }.getOrNull()

    override suspend fun currentUserEmail(session: WebDavSession): String? =
        runCatching { session.ocs.getCurrentUser().email }.getOrNull()

    /** Helper for dialects that opt into [WebDavFeatureKey.QUOTA]. */
    protected suspend fun ocsStorageInfo(session: WebDavSession): Result<CloudStorageInfo> = try {
        val quota = session.ocs.getCurrentUser().quota
        if (quota.total <= 0L) {
            Result.failure(UnsupportedOperationException("Storage quota unavailable"))
        } else {
            Result.success(
                CloudStorageInfo(
                    usedBytes = quota.used,
                    totalBytes = quota.total,
                    usedPercentage = (quota.used.toDouble() / quota.total.toDouble()) * 100.0,
                    usedFormatted = Formatter.formatShortFileSize(session.context, quota.used),
                    totalFormatted = Formatter.formatShortFileSize(session.context, quota.total)
                )
            )
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
