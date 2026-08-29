package com.dot.gallery.core.util.ext

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaTimestampTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver = context.contentResolver
    private val sourceDateModified = 1600000000L // 2020-09-13

    @Test
    fun copiedMediaKeepsTheTimestampOfItsSource() = runBlocking {
        val uri = writeImage()
        try {
            // MediaProvider hands out the time of the write, whatever the app asked for.
            assertTrue(dateModifiedOf(uri) > sourceDateModified)
            assertEquals(4L, resolver.mediaSize(uri))

            assertTrue(
                context.restoreMediaTimestamp(uri, "image/jpeg", sourceDateModified)
            )
            assertEquals(sourceDateModified, dateModifiedOf(uri))
        } finally {
            resolver.delete(uri, null, null)
        }
    }

    @Test
    fun unknownSourceTimestampLeavesTheCopyAlone() = runBlocking {
        val uri = writeImage()
        try {
            val written = dateModifiedOf(uri)
            assertTrue(!context.restoreMediaTimestamp(uri, "image/jpeg", 0L))
            assertEquals(written, dateModifiedOf(uri))
        } finally {
            resolver.delete(uri, null, null)
        }
    }

    private fun writeImage(): Uri {
        val uri = resolver.insert(
            MediaStore.Images.Media.getContentUri("external_primary"),
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "timestamp-${System.nanoTime()}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/ReFraTimestampTest/")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
                put(MediaStore.MediaColumns.DATE_MODIFIED, sourceDateModified)
            }
        )!!
        resolver.openOutputStream(uri)!!.use {
            it.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
        }
        resolver.update(
            uri,
            ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
            null,
            null
        )
        return uri
    }

    private fun dateModifiedOf(uri: Uri): Long = resolver.mediaDateModified(uri)
}
