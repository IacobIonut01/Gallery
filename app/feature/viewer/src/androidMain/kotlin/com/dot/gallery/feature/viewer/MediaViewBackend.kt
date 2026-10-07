/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.core.Settings
import com.dot.gallery.core.decoder.format.ImageReencoder
import com.dot.gallery.core.ui.mediaview.VisualSearchTarget
import com.dot.gallery.data.entity.MediaMetadata
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.Vault
import kotlinx.coroutines.flow.Flow

/**
 * App-provided backend for [MediaViewViewModel]: WorkManager rotation, the people store, and
 * the visual-search preparation pipeline stay app-side behind this bridge — bound in
 * `AppComponent` so the ViewModel itself can live in this module.
 */
interface MediaViewBackend {

    /** Live list of on-device persons whose faces were detected in [mediaId]. */
    fun peopleForMedia(mediaId: Long): Flow<List<PersonInfo>>

    /** Un-assign [mediaIds] from [personId] without deleting the media. */
    suspend fun removeMediaFromPerson(personId: String, mediaIds: List<Long>): Boolean

    /**
     * Enqueues the rotation work for [media] and emits its lifecycle as [RotateUpdate]s until
     * the work finishes (success or failure). Collection cancels observation only.
     */
    fun rotateMedia(media: Media, degrees: Int, forceCopy: Boolean): Flow<RotateUpdate>

    /** Runs the visual-search preparation pipeline; see `VisualSearchLauncher`. */
    suspend fun prepareVisualSearch(
        media: Media,
        metadata: MediaMetadata?,
        currentVault: Vault?,
        positionMs: Long?,
        convertTo: ImageReencoder.ImageWriteFormat?,
        onStage: (VisualSearchStage, Int?) -> Unit,
    ): PreparedVisualSearchImage
}

/** Maps the stored convert-format preference onto a concrete encoder format. */
fun String.toVisualSearchWriteFormat(): ImageReencoder.ImageWriteFormat = when (this) {
    Settings.Misc.VISUAL_SEARCH_FORMAT_PNG -> ImageReencoder.ImageWriteFormat.PNG
    Settings.Misc.VISUAL_SEARCH_FORMAT_WEBP -> ImageReencoder.ImageWriteFormat.WEBP_LOSSY
    else -> ImageReencoder.ImageWriteFormat.JPEG
}

/**
 * Fires `ACTION_SEND` pinned to the resolved provider activity. Call with an Activity context so
 * the provider lands on the viewer's task.
 */
fun Context.launchVisualSearch(target: VisualSearchTarget, uri: Uri, mimeType: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        setClassName(target.packageName, target.activityName)
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        // ClipData carries the grant — a bare EXTRA_STREAM loses it on some targets.
        clipData = ClipData.newUri(contentResolver, target.label, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(intent)
}
