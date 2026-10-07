/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery

import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dot.gallery.core.util.ext.captureDateMillis
import com.dot.gallery.core.util.ext.updateCaptureDate
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.ZoneId


@RunWith(AndroidJUnit4::class)
class CaptureDateExifTest {

    @Test
    fun updateCaptureDateWritesPortableExifFields() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File.createTempFile("capture-date-", ".jpg", context.cacheDir)
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        val zone = ZoneId.of("Europe/Bucharest")
        val expected = LocalDateTime.of(2019, 5, 18, 14, 42, 0)
            .atZone(zone).toInstant().toEpochMilli()

        ExifInterface(file).apply {
            updateCaptureDate(expected, zone)
            saveAttributes()
        }

        ExifInterface(file).let { exif ->
            assertEquals("2019:05:18 14:42:00", exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
            assertEquals("2019:05:18 14:42:00", exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED))
            assertEquals(expected, exif.captureDateMillis())
        }
        file.delete()
    }
}
