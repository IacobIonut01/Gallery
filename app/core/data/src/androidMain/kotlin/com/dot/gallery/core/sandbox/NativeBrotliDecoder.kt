/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.sandbox

import android.app.Service
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Metadata
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import com.drew.metadata.mov.media.QuickTimeVideoDirectory
import com.drew.metadata.mp4.media.Mp4VideoDirectory
import com.drew.metadata.xmp.XmpDirectory
import com.drew.metadata.xmp.XmpReader
import com.dot.gallery.data.entity.isRedactedCoordinate
import com.dot.gallery.domain.model.parseCaptureTimestamp
import com.dot.gallery.core.logging.printError
import com.dot.gallery.core.logging.printWarn
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.io.PushbackInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.dot.gallery.domain.model.Album
import com.dot.gallery.core.util.and
import com.dot.gallery.core.util.like
import com.dot.gallery.core.util.or
import com.dot.gallery.data.entity.MediaMetadata
import com.dot.gallery.cloud.core.matches
import com.dot.gallery.domain.util.isVideo

object NativeBrotliDecoder {
    private val isAvailable = runCatching { System.loadLibrary("metadata_brotli") }.isSuccess

    fun decompress(bytes: ByteArray, maxOutputSize: Int): ByteArray? {
        if (!isAvailable) return null
        return runCatching { nativeDecompress(bytes, maxOutputSize) }.getOrNull()
    }

    private external fun nativeDecompress(bytes: ByteArray, maxOutputSize: Int): ByteArray?
}