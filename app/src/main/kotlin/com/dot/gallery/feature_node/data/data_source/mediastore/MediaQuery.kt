package com.dot.gallery.feature_node.data.data_source.mediastore

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import com.dot.gallery.BuildConfig
import com.dot.gallery.core.util.Query
import com.dot.gallery.core.util.SdkCompat
import com.dot.gallery.core.util.and
import com.dot.gallery.core.util.eq
import com.dot.gallery.core.util.like
import com.dot.gallery.core.util.or

object MediaQuery {
    val MediaStoreFileUri: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

    private val MediaProjectionBase = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DATA,
        MediaStore.Files.FileColumns.RELATIVE_PATH,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.BUCKET_ID,
        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
        MediaStore.Files.FileColumns.DATE_TAKEN,
        MediaStore.Files.FileColumns.DATE_MODIFIED,
        MediaStore.Files.FileColumns.DURATION,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.MIME_TYPE,
    )

    val MediaProjection: Array<String>
        get() = if (SdkCompat.supportsFavorites) {
            MediaProjectionBase + arrayOf(
                MediaStore.Files.FileColumns.IS_FAVORITE,
                MediaStore.Files.FileColumns.IS_TRASHED
            )
        } else {
            MediaProjectionBase
        }

    val MediaProjectionTrash: Array<String>
        get() = if (SdkCompat.supportsTrash) {
            MediaProjectionBase + arrayOf(
                MediaStore.Files.FileColumns.IS_FAVORITE,
                MediaStore.Files.FileColumns.IS_TRASHED,
                MediaStore.Files.FileColumns.DATE_EXPIRES
            )
        } else {
            // Trash is not supported on API 29; return base projection
            MediaProjectionBase
        }

    val MediaMetadataProjection: Array<String>
        get() {
            val base = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DATE_TAKEN,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.DURATION,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.MIME_TYPE,
            )
            return if (SdkCompat.supportsTrash) {
                base + arrayOf(MediaStore.Files.FileColumns.DATE_EXPIRES)
            } else {
                base
            }
        }

    val AlbumsProjection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DATA,
        MediaStore.Files.FileColumns.RELATIVE_PATH,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.BUCKET_ID,
        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
        MediaStore.Files.FileColumns.DATE_TAKEN,
        MediaStore.Files.FileColumns.DATE_MODIFIED,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.MIME_TYPE,
    )

    /**
     * Image formats that Android's media scanner leaves as [MediaStore.Files.FileColumns.MEDIA_TYPE_NONE]
     * (i.e. it does NOT classify them as images), but that ReFra can still decode and display via its
     * custom Sketch/Glide decoders. These rows exist in `MediaStore.Files` with full metadata
     * (bucket, dates, size, favorite/trash flags) — they're just not tagged as images — so surfacing
     * them keeps every MediaStore feature and the same query performance. Only used when
     * [includeUnclassifiedImages] is true (all-files access; never on the Google Play flavor).
     *
     * Bare, lower-case extensions (no leading dot). Every entry maps to a real ReFra decoder:
     *  - `jxl`                                  -> JXL coder (SketchJxlDecoder / Glide, JxlRegionDecoder)
     *  - `jp2 j2k jpf jpx j2c jpc`              -> JPEG 2000 (Jp2ImageDecoder, gemalto jp2)
     *  - `psd psb`                              -> Adobe Photoshop (PsdImageDecoder)
     *  - `apng`                                 -> animated PNG (supportApng); `.png`-named APNGs are
     *                                              already classified as images by MediaStore.
     */
    val ExtraImageExtensions = listOf(
        // JPEG XL
        "jxl",
        // JPEG 2000 family
        "jp2", "j2k", "jpf", "jpx", "j2c", "jpc",
        // Adobe Photoshop
        "psd", "psb",
        // Animated PNG (only the `.apng`-named variant is unclassified)
        "apng",
    )

    /**
     * Whether the current build + runtime state allows us to surface [ExtraImageExtensions] files
     * that MediaStore filed under `MEDIA_TYPE_NONE`.
     *
     * Both conditions are required:
     *  - [BuildConfig.ALLOW_ALL_FILES_ACCESS]: compile-time flavor gate. The Google Play flavor
     *    strips `MANAGE_EXTERNAL_STORAGE` and sets this to `false`, so it is never widened there.
     *  - [SdkCompat.hasFullFileAccess]: the user actually granted All-files access at runtime. With
     *    only scoped `READ_MEDIA_IMAGES`, `MEDIA_TYPE_NONE` rows are invisible regardless of the
     *    query, so widening it would be a no-op (and we avoid the extra predicate entirely).
     *
     * When false, queries behave exactly as before (JXL stays hidden, zero performance impact).
     */
    val includeUnclassifiedImages: Boolean
        get() = BuildConfig.ALLOW_ALL_FILES_ACCESS && SdkCompat.hasFullFileAccess

    /**
     * Normalizes the MIME type reported by MediaStore for unclassified image formats. MediaStore
     * frequently returns a blank or `application/octet-stream` MIME for files like JPEG-XL, which
     * would make downstream image-type checks (and content-URI resolution) treat them as non-images
     * (or worse, as videos). When the file's extension is a known [ExtraImageExtensions] entry and
     * the raw MIME isn't already an image or video type, this returns a canonical `image/<ext>`.
     */
    fun normalizeImageMimeType(pathOrDisplayName: String, rawMimeType: String): String {
        if (rawMimeType.startsWith("image/") || rawMimeType.startsWith("video/")) return rawMimeType
        val ext = pathOrDisplayName.substringAfterLast('.', "").lowercase()
        return if (ext in ExtraImageExtensions) "image/$ext" else rawMimeType
    }

    /**
     * Builds the correct MediaStore content URI for a media row.
     *
     * Files in [ExtraImageExtensions] (e.g. JPEG-XL) are filed by the media scanner under
     * `MEDIA_TYPE_NONE`, so they exist ONLY in the `MediaStore.Files` collection — they are NOT
     * in `Images`/`Video`. Appending their id to `Images.Media.EXTERNAL_CONTENT_URI` yields a URI
     * that resolves to "No item at …" and `openInputStream` throws [java.io.FileNotFoundException]
     * (blank grid tile / viewer). Routing them to the `Files` collection URI resolves correctly and
     * is safe for regular images/videos too (Files is a superset), so it's used whenever the file's
     * extension is one of the unclassified formats regardless of how MediaStore classified the row.
     */
    fun mediaStoreItemUri(id: Long, mimeType: String, pathOrDisplayName: String): Uri {
        val ext = pathOrDisplayName.substringAfterLast('.', "").lowercase()
        val base = when {
            ext in ExtraImageExtensions -> MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            mimeType.contains("image") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        return ContentUris.withAppendedId(base, id)
    }

    object Selection {
        private val imageType =
            MediaStore.Files.FileColumns.MEDIA_TYPE eq MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
        val video =
            MediaStore.Files.FileColumns.MEDIA_TYPE eq MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

        /**
         * `MEDIA_TYPE = NONE AND (DATA LIKE '%.jxl' OR ...)` — the unclassified image files we want
         * to pull back in. Null when there are no [ExtraImageExtensions] configured.
         */
        private val unclassifiedImages: Query?
            get() {
                val extClause = ExtraImageExtensions
                    .map { MediaStore.Files.FileColumns.DATA like "%.$it" }
                    .reduceOrNull { acc, q -> acc or q } ?: return null
                return (MediaStore.Files.FileColumns.MEDIA_TYPE eq
                        MediaStore.Files.FileColumns.MEDIA_TYPE_NONE) and extClause
            }

        /**
         * Image selection. Widened to also include [unclassifiedImages] (e.g. JPEG-XL) when
         * [includeUnclassifiedImages] is true; otherwise identical to the plain
         * `MEDIA_TYPE = IMAGE` filter.
         */
        val image: Query
            get() = if (includeUnclassifiedImages) {
                unclassifiedImages?.let { imageType or it } ?: imageType
            } else {
                imageType
            }

        val imageOrVideo: Query get() = image or video
    }
}