/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.core.ml.CutoutHelper
import com.dot.gallery.core.ml.ModelManager
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CutoutEngineTest {

    @Test
    fun testCutoutSessionLifecycleAndInference() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        
        // 1. Initialize ModelManager
        val modelManager = ModelManager(appContext)
        modelManager.initializeModels()
        
        // Ensure models are downloaded/ready (for withML builds they should be ready)
        assertTrue("ModelManager should be ready", modelManager.isReady)

        // 2. Create and insert a dummy image into MediaStore
        val testMedia = createAndInsertTestImage(appContext)
        
        // 3. Create CutoutSession
        val session = CutoutHelper.CutoutSession(appContext, testMedia, modelManager)
        
        try {
            // 4. Test SAM Encoder initialization & run
            val initOk = session.initAndRunEncoder()
            assertTrue("Cutout Session Encoder should initialize successfully", initOk)
            
            // 5. Test SAM Decoder inference with a center point prompt
            val centerPoint = CutoutHelper.PromptPoint(256f, 256f, isPositive = true)
            val samResult = session.runDecoder(listOf(centerPoint))
            assertNotNull("SAM Decoder should return a valid cutout result", samResult)
            
            // 6. Test finalizeCutout (pass-through of the cached SAM result)
            val finalResult = session.finalizeCutout(samResult!!.originalBounds)
            assertNotNull("finalizeCutout should return a valid cutout result", finalResult)
            assertTrue("finalizeCutout should return the cached SAM result", samResult === finalResult)
            
            // Cleanup bitmap (since they are the same object, only recycle once)
            samResult.bitmap.recycle()
        } finally {
            session.close()
            // Delete the dummy image from MediaStore
            appContext.contentResolver.delete(testMedia.uri, null, null)
        }
    }

    private fun createAndInsertTestImage(context: Context): Media.UriMedia {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLUE)

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "test_cutout_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Test")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to insert test image into MediaStore")

        resolver.openOutputStream(uri).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out!!)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE
        )

        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val label = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                val relPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH))
                val bucketId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID))
                val bucketLabel = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
                val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))

                bitmap.recycle()
                return Media.UriMedia(
                    id = id,
                    label = label ?: "image.png",
                    uri = uri,
                    path = path ?: "",
                    relativePath = relPath ?: "",
                    albumID = bucketId,
                    albumLabel = bucketLabel ?: "Test",
                    timestamp = timestamp,
                    fullDate = "June 23, 2026",
                    mimeType = mimeType ?: "image/png",
                    favorite = 0,
                    trashed = 0,
                    size = size
                )
            }
        }
        bitmap.recycle()
        throw IllegalStateException("Failed to query inserted test image")
    }
}
