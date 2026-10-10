/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.dot.gallery.docs.showcase.generated.resources.Res
import com.dot.gallery.domain.date.getDate
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.domain.uri.parseUri
import java.io.File
import kotlinx.coroutines.runBlocking

/**
 * Materialises the bundled docs photographs/clip into **this package's own**
 * MediaStore rows so the real viewer chain (Sketch decode, ExoPlayer, local
 * action policies) runs against genuine `content://` URIs — a `fixture://`
 * model cannot drive either decoder.
 *
 * Idempotent: rows owned by `context.packageName` under
 * `Pictures/ReFraDocsPreview/` / `Movies/ReFraDocsPreview/` are reused;
 * foreign rows are never touched. Timestamps anchor to [DOCS_DEMO_EPOCH_SECONDS]
 * so captures do not drift. Emulator-only fixture writes — nothing here runs in
 * the shipped app.
 */
object DocsMediaStoreFixture {

    private const val IMAGE_RELATIVE_PATH = "Pictures/ReFraDocsPreview/"
    private const val VIDEO_RELATIVE_PATH = "Movies/ReFraDocsPreview/"

    private val PROJECTION = arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.RELATIVE_PATH,
        MediaStore.MediaColumns.BUCKET_ID,
        MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
        MediaStore.MediaColumns.DATE_TAKEN,
        MediaStore.MediaColumns.DATE_MODIFIED,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.MIME_TYPE,
    )

    private const val SELECTION =
        "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND " +
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND " +
            "${MediaStore.MediaColumns.OWNER_PACKAGE_NAME} = ?"

    private data class Row(
        val id: Long,
        val name: String,
        val rel: String,
        val bucketId: Long,
        val bucket: String,
        val takenMs: Long,
        val modifiedSec: Long,
        val size: Long,
        val mime: String,
    )

    /** One bundled image under `Pictures/ReFraDocsPreview/`. */
    fun image(
        context: Context,
        resourcePath: String,
        displayName: String,
        ageDays: Long,
        favorite: Boolean,
        mimeType: String = "image/jpeg",
    ): UriMedia = ensure(
        context = context,
        collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        relativePath = IMAGE_RELATIVE_PATH,
        displayName = displayName,
        mimeType = mimeType,
        bytes = { runBlocking { Res.readBytes(resourcePath) } },
        ageDays = ageDays,
        favorite = favorite,
        durationMs = null,
    )

    /** The bundled CC0 flower clip under `Movies/ReFraDocsPreview/`. */
    fun video(
        context: Context,
        resourcePath: String,
        displayName: String,
        ageDays: Long,
    ): UriMedia {
        val file = materialise(context, resourcePath)
        val retriever = MediaMetadataRetriever()
        val durationMs = try {
            retriever.setDataSource(file.absolutePath)
            requireNotNull(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
            ) { "docs fixture clip has no duration metadata: $resourcePath" }.also {
                require(it > 0) { "docs fixture clip duration is $it ms: $resourcePath" }
            }
        } finally {
            retriever.release()
        }
        return ensure(
            context = context,
            collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            relativePath = VIDEO_RELATIVE_PATH,
            displayName = displayName,
            mimeType = "video/mp4",
            bytes = { runBlocking { Res.readBytes(resourcePath) } },
            ageDays = ageDays,
            favorite = false,
            durationMs = durationMs,
        )
    }

    /**
     * A generated (non-bundled) image row — e.g. an edited variant produced at
     * runtime — under the same owned `Pictures/ReFraDocsPreview/` collection.
     * The row is keyed by display name, so the same recipe stays idempotent.
     */
    fun generatedImage(
        context: Context,
        displayName: String,
        bytes: ByteArray,
        ageDays: Long,
        favorite: Boolean = false,
    ): UriMedia = ensure(
        context = context,
        collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        relativePath = IMAGE_RELATIVE_PATH,
        displayName = displayName,
        mimeType = "image/jpeg",
        bytes = { bytes },
        ageDays = ageDays,
        favorite = favorite,
        durationMs = null,
    )

    /** The raw fixture bytes as a private-cache file (frame extraction etc.). */
    fun materialise(context: Context, resourcePath: String): File =
        File(context.cacheDir, "docs_fixture_${resourcePath.substringAfterLast('/')}").also { file ->
            if (!file.exists() || file.length() == 0L) {
                file.writeBytes(runBlocking { Res.readBytes(resourcePath) })
            }
        }

    private fun ensure(
        context: Context,
        collection: Uri,
        relativePath: String,
        displayName: String,
        mimeType: String,
        bytes: () -> ByteArray,
        ageDays: Long,
        favorite: Boolean,
        durationMs: Long?,
    ): UriMedia {
        val resolver = context.contentResolver
        val selectionArgs = arrayOf(relativePath, displayName, context.packageName)
        val takenMs = (DOCS_DEMO_EPOCH_SECONDS - ageDays * 86_400L) * 1_000L

        val contentUri = findOwned(resolver, collection, selectionArgs) ?: run {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
                if (favorite) put(MediaStore.MediaColumns.IS_FAVORITE, 1)
            }
            val uri = requireNotNull(resolver.insert(collection, values)) {
                "docs fixture insert returned null for $displayName"
            }
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes())
                } ?: error("docs fixture could not open $uri for writing")
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                    null,
                    null,
                )
                uri
            } catch (e: Exception) {
                // Remove only the half-written row we just created; reused rows
                // never reach this path.
                resolver.delete(uri, null, null)
                throw e
            }
        }

        // Re-read the real row so id/bucket/size come from MediaStore itself.
        val row = resolver.query(contentUri, PROJECTION, null, null, null)?.use { c ->
            if (!c.moveToFirst()) return@use null
            Row(
                id = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)),
                name = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)),
                rel = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH))
                    ?: relativePath,
                bucketId = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)),
                bucket = c.getString(
                    c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                ) ?: "ReFraDocsPreview",
                takenMs = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)),
                modifiedSec = c.getLong(
                    c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                ),
                size = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)),
                mime = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)),
            )
        } ?: error("docs fixture row vanished for $contentUri")

        val path = File(
            Environment.getExternalStorageDirectory(),
            "${row.rel}${row.name}".removePrefix("/"),
        ).absolutePath
        // MediaStore keeps no caller-supplied dates (the publish scan clears
        // DATE_TAKEN and rewrites DATE_MODIFIED from the file mtime), so the
        // model carries the intended docs timestamp; row supplies id/uri/size.
        val takenSec = takenMs / 1_000L
        return UriMedia(
            id = row.id,
            label = row.name,
            uri = parseUri(contentUri.toString()),
            path = path,
            relativePath = row.rel,
            albumID = row.bucketId,
            albumLabel = row.bucket,
            timestamp = takenSec,
            takenTimestamp = takenMs,
            fullDate = takenSec.getDate("EEE, MMM d, yyyy"),
            mimeType = row.mime,
            favorite = if (favorite) 1 else 0,
            trashed = 0,
            size = row.size,
            duration = durationMs?.toString(),
        )
    }

    private fun findOwned(
        resolver: android.content.ContentResolver,
        collection: Uri,
        selectionArgs: Array<String>,
    ): Uri? = resolver.query(collection, PROJECTION, SELECTION, selectionArgs, null)?.use { c ->
        if (c.moveToFirst()) {
            val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
            ContentUris.withAppendedId(collection, id)
        } else null
    }
}

/** Resolved per composition; MediaStore writes happen once, reads stay cheap. */
@Composable
fun rememberDocsMediaStoreImage(
    resourcePath: String,
    displayName: String,
    ageDays: Long = 0L,
    favorite: Boolean = false,
    mimeType: String = "image/jpeg",
): UriMedia {
    val context = LocalContext.current
    return remember(resourcePath, displayName, mimeType) {
        DocsMediaStoreFixture.image(context, resourcePath, displayName, ageDays, favorite, mimeType)
    }
}

@Composable
fun rememberDocsMediaStoreVideo(
    resourcePath: String,
    displayName: String,
    ageDays: Long = 0L,
): UriMedia {
    val context = LocalContext.current
    return remember(resourcePath, displayName) {
        DocsMediaStoreFixture.video(context, resourcePath, displayName, ageDays)
    }
}
