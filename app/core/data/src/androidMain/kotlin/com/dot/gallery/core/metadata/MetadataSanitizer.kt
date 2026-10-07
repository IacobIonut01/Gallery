package com.dot.gallery.core.metadata

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.getUri
import com.dot.gallery.core.logging.printError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.Properties
import java.util.UUID
import javax.inject.Inject
import com.dot.gallery.core.injection.Singleton
import kotlin.coroutines.coroutineContext
import com.dot.gallery.domain.uri.query




interface MetadataSanitizer {
    suspend fun probe(media: Media): SanitizationCapability
    suspend fun sanitize(
        media: Media,
        mode: MetadataRemovalMode,
        saveMode: MetadataSaveMode = MetadataSaveMode.SAVE_COPY
    ): SanitizationResult
    suspend fun recoverPendingTransactions()
}