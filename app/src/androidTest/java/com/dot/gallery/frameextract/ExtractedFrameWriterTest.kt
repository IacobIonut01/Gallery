package com.dot.gallery.frameextract

import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dot.gallery.feature_node.presentation.frameextract.ExtractedFrameWriter
import com.dot.gallery.feature_node.presentation.frameextract.FrameExportFormat
import com.dot.gallery.feature_node.presentation.frameextract.FrameIdentity
import com.dot.gallery.feature_node.presentation.frameextract.FrameSourceKind
import com.dot.gallery.feature_node.presentation.frameextract.FrameSourceSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExtractedFrameWriterTest {
    @Test
    fun writesPendingSafeJpegWithNormalizedMetadata() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resolver = context.contentResolver
        val bitmap = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888)
        val source = FrameSourceSpec(
            mediaId = 1,
            label = "test-video.mp4",
            uri = "content://test/video",
            mimeType = "video/mp4",
            sourceKind = FrameSourceKind.DOCUMENT,
            relativePath = "Movies/Test",
            captureTimestampMs = 1_700_000_000_000L,
            latitude = 47.0,
            longitude = 27.0,
        )
        val result = ExtractedFrameWriter(resolver).write(
            bitmap,
            source,
            FrameIdentity(1, 1_000_000),
            FrameExportFormat.JPEG,
        )
        try {
            resolver.query(
                result.uri,
                arrayOf(
                    MediaStore.Images.Media.MIME_TYPE,
                    MediaStore.Images.Media.WIDTH,
                    MediaStore.Images.Media.HEIGHT,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.IS_PENDING,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("image/jpeg", cursor.getString(0))
                assertEquals(32, cursor.getInt(1))
                assertEquals(24, cursor.getInt(2))
                assertEquals(source.captureTimestampMs, cursor.getLong(3))
                assertEquals(0, cursor.getInt(4))
            }
            resolver.openFileDescriptor(result.uri, "r")?.use { descriptor ->
                val exif = ExifInterface(descriptor.fileDescriptor)
                assertEquals(
                    ExifInterface.ORIENTATION_NORMAL,
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1),
                )
                assertNotNull(exif.latLong)
            }
        } finally {
            resolver.delete(result.uri, null, null)
            bitmap.recycle()
        }
    }
}
