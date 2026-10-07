package com.dot.gallery.domain.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.xmp.XmpDirectory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.coroutines.coroutineContext
import com.dot.gallery.domain.model.Media
import com.dot.gallery.cloud.core.matches
import com.dot.gallery.core.util.or
import com.dot.gallery.core.logging.printDebug
import com.dot.gallery.core.logging.printWarning




/**
 * Describes the embedded video inside a Motion Photo / Microvideo file.
 *
 * @param videoOffset Byte offset from **end** of file where the MP4 stream starts.
 * @param presentationTimestampUs The "favourite shot" / key-frame timestamp in µs.
 *        A value of -1 means the field was absent.
 */
data class MotionPhotoInfo(
    val videoOffset: Long,
    val presentationTimestampUs: Long = -1L
)