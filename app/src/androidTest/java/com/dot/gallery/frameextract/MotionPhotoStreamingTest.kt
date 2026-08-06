package com.dot.gallery.frameextract

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dot.gallery.feature_node.domain.util.MotionPhotoHelper
import com.dot.gallery.feature_node.domain.util.MotionPhotoInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MotionPhotoStreamingTest {
    @Test
    fun extractsOnlyEmbeddedVideoAndCleansTemporaryOutput() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val directory = File(context.cacheDir, "frameextract-test").apply { mkdirs() }
        val source = File(directory, "synthetic-motion.jpg")
        val prefix = ByteArray(1024 * 1024) { (it % 251).toByte() }
        val video = byteArrayOf(
            0, 0, 0, 24,
            'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(),
            'i'.code.toByte(), 's'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
            1, 2, 3, 4, 5, 6, 7, 8,
        )
        source.outputStream().use { output ->
            output.write(prefix)
            output.write(video)
        }

        val extracted = MotionPhotoHelper.extractVideoFromFile(
            source,
            MotionPhotoInfo(videoOffset = video.size.toLong()),
            directory,
        )

        assertNotNull(extracted)
        assertArrayEquals(video, extracted!!.readBytes())
        extracted.delete()
        source.delete()
        directory.delete()
        assertFalse(extracted.exists())
    }
}
