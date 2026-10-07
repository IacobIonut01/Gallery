/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.mediastore.queries

import android.content.ContentResolver
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.dot.gallery.domain.date.FULL_DATE_FORMAT
import com.dot.gallery.core.util.MediaStoreBuckets
import com.dot.gallery.core.util.PickerUtils
import com.dot.gallery.core.util.Query
import com.dot.gallery.core.util.SdkCompat
import com.dot.gallery.core.util.and
import com.dot.gallery.core.util.eq
import com.dot.gallery.core.util.join
import com.dot.gallery.core.util.or
import com.dot.gallery.core.util.ext.mapEachRow
import com.dot.gallery.core.util.ext.queryFlow
import com.dot.gallery.core.util.ext.querySteppedFlow
import com.dot.gallery.core.util.ext.tryGetLong
import com.dot.gallery.core.util.ext.tryGetString
import com.dot.gallery.data.mediastore.MediaQuery
import com.dot.gallery.domain.model.CaptureTimeOrigin
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.domain.uri.path
import com.dot.gallery.domain.util.isFavorite
import com.dot.gallery.domain.util.isTrashed
import com.dot.gallery.domain.date.parseTimestampFromFilename
import com.dot.gallery.domain.date.getDate


private fun mediaBucketQuery(bucketCount: Int): Query? = List(bucketCount) {
    MediaStore.Files.FileColumns.BUCKET_ID eq Query.ARG
}.join(Query::or)

fun mediaBucketSelection(bucketCount: Int): String =
    mediaBucketQuery(bucketCount)?.build().orEmpty()

internal const val MEDIA_IDS_MAX = 100

fun validatedMediaIds(mediaIds: Set<Long>?): List<Long>? {
    if (mediaIds == null) return null
    require(mediaIds.size <= MEDIA_IDS_MAX) { "mediaIds supports at most $MEDIA_IDS_MAX ids" }
    require(mediaIds.all { it >= 0 }) { "mediaIds must be local MediaStore ids" }
    return mediaIds.sorted()
}

fun mediaIdsQuery(orderedMediaIds: List<Long>): Query? =
    List(orderedMediaIds.size) { MediaStore.Files.FileColumns._ID eq Query.ARG }.join(Query::or)

/**
 * Media flow
 *
 * This class is responsible for fetching media from the media store
 *
 * @property contentResolver
 * @property buckedId
 * @property mimeType
 */
class MediaFlow(
    private val contentResolver: ContentResolver,
    private val buckedId: Long,
    private val mimeType: String? = null,
    private val skipBatching: Boolean = false,
    private val bucketIds: Set<Long> = setOf(buckedId),
    private val queryLimit: Int? = null,
    private val mediaIds: Set<Long>? = null
) : QueryFlow<UriMedia>() {
    init {
        require(queryLimit == null || queryLimit > 0) { "queryLimit must be positive" }
        validatedMediaIds(mediaIds)
        assert(buckedId != MediaStoreBuckets.MEDIA_STORE_BUCKET_PLACEHOLDER.id || bucketIds.isEmpty()) {
            "MEDIA_STORE_BUCKET_PLACEHOLDER found"
        }
    }

    override fun flowCursor(): Flow<Cursor?> {
        if (bucketIds.isEmpty()) return flowOf(null)
        val orderedMediaIds = validatedMediaIds(mediaIds)
        if (orderedMediaIds != null && orderedMediaIds.isEmpty()) return flowOf(null)
        // Trash and Favorites are not supported on API 29
        if (!SdkCompat.supportsTrash && buckedId == MediaStoreBuckets.MEDIA_STORE_BUCKET_TRASH.id) {
            return flowOf(null)
        }
        if (!SdkCompat.supportsFavorites && buckedId == MediaStoreBuckets.MEDIA_STORE_BUCKET_FAVORITES.id) {
            return flowOf(null)
        }

        val uri = MediaQuery.MediaStoreFileUri
        val projection = when(buckedId) {
            MediaStoreBuckets.MEDIA_STORE_BUCKET_TRASH.id ->
                MediaQuery.MediaProjectionTrash
            else ->
                MediaQuery.MediaProjection
        }
        val imageOrVideo = PickerUtils.mediaTypeFromGenericMimeType(mimeType)?.let {
            when (it) {
                MediaType.IMAGE -> MediaQuery.Selection.image
                MediaType.VIDEO -> MediaQuery.Selection.video
            }
        } ?: when (buckedId) {
            MediaStoreBuckets.MEDIA_STORE_BUCKET_PHOTOS.id -> MediaQuery.Selection.image
            MediaStoreBuckets.MEDIA_STORE_BUCKET_VIDEOS.id -> MediaQuery.Selection.video
            else -> MediaQuery.Selection.imageOrVideo
        }
        val orderedBucketIds = bucketIds.sorted()
        val usesBucketFilter = MediaStoreBuckets.entries.none { it.id == buckedId }
        val albumFilter = when (buckedId) {
            MediaStoreBuckets.MEDIA_STORE_BUCKET_FAVORITES.id ->
                if (SdkCompat.supportsFavorites)
                    MediaStore.Files.FileColumns.IS_FAVORITE eq 1
                else null

            MediaStoreBuckets.MEDIA_STORE_BUCKET_TRASH.id ->
                if (SdkCompat.supportsTrash)
                    MediaStore.Files.FileColumns.IS_TRASHED eq 1
                else null

            MediaStoreBuckets.MEDIA_STORE_BUCKET_TIMELINE.id,
            MediaStoreBuckets.MEDIA_STORE_BUCKET_PHOTOS.id,
            MediaStoreBuckets.MEDIA_STORE_BUCKET_VIDEOS.id -> null

            else -> mediaBucketQuery(orderedBucketIds.size)
        }
        val rawMimeType = mimeType?.takeIf { PickerUtils.isMimeTypeNotGeneric(it) }
        val mimeTypeQuery = rawMimeType?.let {
            MediaStore.Files.FileColumns.MIME_TYPE eq Query.ARG
        }
        val idQuery = orderedMediaIds?.let(::mediaIdsQuery)
        val selection = listOfNotNull(
            imageOrVideo,
            albumFilter,
            mimeTypeQuery,
            idQuery,
        ).join(Query::and)?.build()
        val selectionArgs = buildList {
            if (usesBucketFilter) addAll(orderedBucketIds.map(Long::toString))
            rawMimeType?.let(::add)
            orderedMediaIds?.let { ids -> addAll(ids.map(Long::toString)) }
        }.toTypedArray()

        val sortOrder = when {
            queryLimit != null ->
                "COALESCE(${MediaStore.Files.FileColumns.DATE_TAKEN} / 1000, " +
                    "${MediaStore.Files.FileColumns.DATE_MODIFIED}) DESC, " +
                    "${MediaStore.Files.FileColumns._ID} DESC"
            buckedId == MediaStoreBuckets.MEDIA_STORE_BUCKET_TRASH.id ->
                if (SdkCompat.supportsTrash) "${MediaStore.Files.FileColumns.DATE_EXPIRES} DESC"
                else "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

            else -> "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        }

        val queryArgs = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
            queryLimit?.let { putString(ContentResolver.QUERY_ARG_SQL_LIMIT, it.toString()) }

            // Exclude trashed media unless we want data for the trashed album
            // QUERY_ARG_MATCH_TRASHED is only available on API 30+
            if (SdkCompat.supportsTrash) {
                putInt(
                    MediaStore.QUERY_ARG_MATCH_TRASHED, when (buckedId) {
                        MediaStoreBuckets.MEDIA_STORE_BUCKET_TRASH.id -> MediaStore.MATCH_ONLY

                        else -> MediaStore.MATCH_EXCLUDE
                    }
                )
            }
        }

        return if (queryLimit != null || skipBatching || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
             contentResolver.queryFlow(
                uri,
                projection,
                queryArgs,
            )
        } else contentResolver.querySteppedFlow(
            uri,
            projection,
            queryArgs,
        )
    }

    override fun flowData() = flowCursor().mapEachRow(
        when (buckedId) {
            MediaStoreBuckets.MEDIA_STORE_BUCKET_TRASH.id -> MediaQuery.MediaProjectionTrash
            else -> MediaQuery.MediaProjection
        }
    ) { it, indexCache ->
        var i = 0

        val id = it.getLong(indexCache[i++])
        val volumeName = it.tryGetString(indexCache[i++]).orEmpty()
            .ifEmpty { MediaStore.VOLUME_EXTERNAL }
        val path = it.tryGetString(indexCache[i++]).orEmpty()
        val relativePath = it.tryGetString(indexCache[i++]).orEmpty()
        val title = it.tryGetString(indexCache[i++]).orEmpty()
        val albumID = it.getLong(indexCache[i++])
        val albumLabel = it.tryGetString(indexCache[i++], Build.MODEL)
        val mediaStoreTakenTimestamp = it.tryGetLong(indexCache[i++])?.takeIf { value -> value != 0L }
        val filenameTimestamp = if (mediaStoreTakenTimestamp == null) {
            title.parseTimestampFromFilename()
        } else null
        val takenTimestamp = mediaStoreTakenTimestamp ?: filenameTimestamp
        val captureTimeOrigin = when {
            mediaStoreTakenTimestamp != null -> CaptureTimeOrigin.MEDIA_STORE
            filenameTimestamp != null -> CaptureTimeOrigin.FILENAME
            else -> CaptureTimeOrigin.MODIFIED_FALLBACK
        }
        val modifiedTimestamp = it.getLong(indexCache[i++])
        val duration = it.tryGetString(indexCache[i++])
        val size = it.getLong(indexCache[i++])
        val mimeType = MediaQuery.normalizeImageMimeType(
            path,
            it.tryGetString(indexCache[i++]).orEmpty()
        )
        // IS_FAVORITE and IS_TRASHED are only available on API 30+
        val isFavorite = if (SdkCompat.supportsFavorites) it.getInt(indexCache[i++]) else 0
        val isTrashAlbum = buckedId == MediaStoreBuckets.MEDIA_STORE_BUCKET_TRASH.id
        val isTrashed = if (SdkCompat.supportsTrash) it.getInt(indexCache[if (isTrashAlbum) i++ else i]) else 0
        val expiryTimestamp = if (isTrashAlbum && SdkCompat.supportsTrash) it.tryGetLong(indexCache[i]) else null
        val uri = MediaQuery.mediaStoreItemUri(id, mimeType, path, volumeName)
        val formattedDate = (takenTimestamp?.div(1000) ?: modifiedTimestamp).getDate(FULL_DATE_FORMAT)
        UriMedia(
            id = id,
            label = title,
            uri = uri,
            path = path,
            relativePath = relativePath,
            albumID = albumID,
            albumLabel = albumLabel ?: Build.MODEL,
            timestamp = modifiedTimestamp,
            takenTimestamp = takenTimestamp,
            captureTimeOrigin = captureTimeOrigin,
            expiryTimestamp = expiryTimestamp,
            fullDate = formattedDate,
            duration = duration,
            favorite = isFavorite,
            trashed = isTrashed,
            size = size,
            mimeType = mimeType
        )
    }
}