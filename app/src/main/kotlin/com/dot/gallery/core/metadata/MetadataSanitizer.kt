package com.dot.gallery.core.metadata

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.getUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.Properties
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

interface MetadataSanitizer {
    suspend fun probe(media: Media): SanitizationCapability
    suspend fun sanitize(media: Media, mode: MetadataRemovalMode): SanitizationResult
    suspend fun recoverPendingTransactions()
}

@Singleton
class AndroidMetadataSanitizer @Inject constructor(
    @param:ApplicationContext private val context: Context
) : MetadataSanitizer {
    private val resolver get() = context.contentResolver
    private val transactionDirectory = File(context.noBackupFilesDir, "metadata-rewrite")

    override suspend fun probe(media: Media): SanitizationCapability = withContext(Dispatchers.IO) {
        val uri = media.getUri()
        val format = resolver.openInputStream(uri)?.use {
            MediaFormatDetector.detect(it, media.mimeType, media.label)
        } ?: MediaContainerFormat.UNKNOWN
        if (format == MediaContainerFormat.JPEG && isMotionPhoto(uri)) {
            return@withContext SanitizationCapability(
                format = format,
                supportedModes = emptySet(),
                limitation = "Motion Photo sanitization is unavailable until its embedded video and linkage can be preserved."
            )
        }
        val base = MetadataCapabilities.forFormat(format)
        if (format in setOf(MediaContainerFormat.JPEG, MediaContainerFormat.PNG, MediaContainerFormat.WEBP) &&
            (hasOpaqueSelectiveMetadata(uri, format) || hasEmbeddedExifThumbnail(uri))
        ) {
            return@withContext base.copy(
                supportedModes = base.supportedModes.intersect(setOf(MetadataRemovalMode.EVERYTHING)),
                limitation = "This file contains nested or opaque metadata that can only be removed safely with Everything."
            )
        }
        base
    }

    override suspend fun sanitize(media: Media, mode: MetadataRemovalMode): SanitizationResult = withContext(Dispatchers.IO) {
        val capability = probe(media)
        if (!capability.supports(mode)) {
            return@withContext SanitizationResult.Unsupported(
                capability.format,
                capability.limitation ?: "The selected metadata mode is not supported."
            )
        }
        transactionDirectory.mkdirs()
        val required = media.size.coerceAtLeast(1L) * 2L + MINIMUM_FREE_MARGIN
        val available = transactionDirectory.usableSpace
        if (available < required) {
            return@withContext SanitizationResult.InsufficientSpace(required, available)
        }

        val id = UUID.randomUUID().toString()
        val backup = File(transactionDirectory, "$id.original")
        val candidate = File(transactionDirectory, "$id.candidate")
        val journal = File(transactionDirectory, "$id.properties")
        var commitStarted = false
        var transactionResolved = false
        try {
            copyUriToFile(media.getUri(), backup)
            val actualRequired = backup.length() + MINIMUM_FREE_MARGIN
            val actualAvailable = transactionDirectory.usableSpace
            if (actualAvailable < actualRequired) {
                return@withContext SanitizationResult.InsufficientSpace(actualRequired, actualAvailable)
            }
            coroutineContext.ensureActive()
            val format = FileInputStream(backup).use {
                MediaFormatDetector.detect(it, media.mimeType, media.label)
            }
            if (format != capability.format) {
                return@withContext SanitizationResult.MalformedInput("Media format changed while preparing sanitization.")
            }
            val beforeFingerprint = MediaEssenceFingerprint.calculate(backup, format)
            LosslessMetadataRewriter.rewrite(backup, candidate, format, mode)
            FileOutputStream(candidate, true).use { it.fd.sync() }
            if (!MetadataVerifier.verify(candidate, format, mode)) {
                return@withContext SanitizationResult.VerificationFailed("The sanitized candidate still contains selected metadata.")
            }
            val afterFingerprint = MediaEssenceFingerprint.calculate(candidate, format)
            if (beforeFingerprint != afterFingerprint) {
                return@withContext SanitizationResult.VerificationFailed("Encoded media payload changed during sanitization.")
            }
            writeJournal(
                journal,
                media.getUri(),
                backup,
                candidate,
                sha256(backup),
                media.timestamp,
                media.takenTimestamp,
                "prepared"
            )
            commitStarted = true
            writeFileToUri(candidate, media.getUri())
            updateJournalPhase(journal, "committed")
            if (sha256(media.getUri()) != sha256(candidate)) {
                throw IOException("Committed file does not match the verified candidate")
            }
            if (!verifyCommitted(media.getUri(), media, format, mode, afterFingerprint)) {
                throw IOException("Committed file failed metadata or payload verification")
            }
            resolver.update(
                media.getUri(),
                ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                    put(MediaStore.MediaColumns.DATE_MODIFIED, media.timestamp)
                    media.takenTimestamp?.let { put(MediaStore.MediaColumns.DATE_TAKEN, it) }
                },
                null,
                null
            )
            transactionResolved = true
            SanitizationResult.Success(
                mode = mode,
                format = format,
                removedCategories = MetadataPolicy.forMode(mode).removedCategories,
                retainedRequiredMetadata = capability.retainedRequiredMetadata,
                bytesBefore = backup.length(),
                bytesAfter = candidate.length()
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            transactionResolved = !commitStarted || withContext(NonCancellable) {
                restore(backup, media.getUri(), media.timestamp, media.takenTimestamp)
            }
            SanitizationResult.Cancelled
        } catch (security: SecurityException) {
            transactionResolved = !commitStarted || withContext(NonCancellable) {
                restore(backup, media.getUri(), media.timestamp, media.takenTimestamp)
            }
            SanitizationResult.PermissionDenied
        } catch (error: Throwable) {
            val rolledBack = if (commitStarted) withContext(NonCancellable) {
                restore(backup, media.getUri(), media.timestamp, media.takenTimestamp)
            } else true
            transactionResolved = rolledBack
            SanitizationResult.CommitFailed(error.message ?: error.javaClass.simpleName, rolledBack)
        } finally {
            if (!commitStarted || transactionResolved) {
                backup.delete()
                candidate.delete()
                journal.delete()
            }
        }
    }

    override suspend fun recoverPendingTransactions() = withContext(Dispatchers.IO) {
        if (!transactionDirectory.exists()) return@withContext
        transactionDirectory.listFiles { file -> file.extension == "properties" }.orEmpty().forEach { journal ->
            val properties = runCatching { journal.readProperties() }.getOrNull() ?: return@forEach
            val uri = properties.getProperty("uri")?.let(Uri::parse) ?: return@forEach
            val backup = properties.getProperty("backup")?.let(::File) ?: return@forEach
            val expected = properties.getProperty("backupSha256") ?: return@forEach
            val dateModified = properties.getProperty("dateModified")?.toLongOrNull()
            val dateTaken = properties.getProperty("dateTaken")?.toLongOrNull()
            if (backup.isFile && runCatching { sha256(backup) == expected }.getOrDefault(false)) {
                restore(backup, uri, dateModified, dateTaken)
                if (runCatching { sha256(uri) == expected }.getOrDefault(false)) {
                    File(properties.getProperty("candidate").orEmpty()).delete()
                    backup.delete()
                    journal.delete()
                }
            }
        }
    }

    private fun verifyCommitted(
        uri: Uri,
        media: Media,
        format: MediaContainerFormat,
        mode: MetadataRemovalMode,
        expectedFingerprint: String
    ): Boolean {
        val verification = File.createTempFile("metadata-verify", ".tmp", transactionDirectory)
        return try {
            copyUriToFile(uri, verification)
            FileInputStream(verification).use {
                MediaFormatDetector.detect(it, media.mimeType, media.label)
            } == format &&
                MetadataVerifier.verify(verification, format, mode) &&
                MediaEssenceFingerprint.calculate(verification, format) == expectedFingerprint
        } finally {
            verification.delete()
        }
    }

    private fun hasEmbeddedExifThumbnail(uri: Uri): Boolean = runCatching {
        resolver.openInputStream(uri)?.use { ExifInterface(it).hasThumbnail() } == true
    }.getOrDefault(false)

    private fun hasOpaqueSelectiveMetadata(uri: Uri, format: MediaContainerFormat): Boolean {
        val tokens = when (format) {
            MediaContainerFormat.JPEG -> listOf(
                "http://ns.adobe.com/xap/1.0/".toByteArray(),
                "http://ns.adobe.com/xmp/extension/".toByteArray(),
                "Photoshop 3.0".toByteArray()
            )
            MediaContainerFormat.PNG -> listOf(
                "iTXt".toByteArray(),
                "tEXt".toByteArray(),
                "zTXt".toByteArray()
            )
            MediaContainerFormat.WEBP -> listOf("XMP ".toByteArray())
            else -> emptyList()
        }
        if (tokens.isEmpty()) return false
        return resolver.openInputStream(uri)?.buffered()?.use { input ->
            val longest = tokens.maxOf { it.size }
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE + longest)
            var carried = 0
            while (true) {
                val count = input.read(buffer, carried, DEFAULT_BUFFER_SIZE)
                if (count < 0) return@use false
                val total = carried + count
                if (tokens.any { token -> buffer.contains(token, total) }) return@use true
                carried = minOf(longest - 1, total)
                buffer.copyInto(buffer, 0, total - carried, total)
            }
            @Suppress("UNREACHABLE_CODE")
            false
        } ?: false
    }

    private fun ByteArray.contains(token: ByteArray, length: Int): Boolean {
        if (token.isEmpty() || token.size > length) return false
        for (start in 0..length - token.size) {
            if (token.indices.all { this[start + it] == token[it] }) return true
        }
        return false
    }

    private fun isMotionPhoto(uri: Uri): Boolean = resolver.openInputStream(uri)?.buffered()?.use { input ->
        val bytes = ByteArray(MOTION_PHOTO_SNIFF_BYTES)
        val count = input.read(bytes).coerceAtLeast(0)
        val header = bytes.copyOf(count).toString(Charsets.ISO_8859_1)
        "GCamera:MotionPhoto" in header ||
            "Camera:MotionPhoto" in header ||
            "GCamera:MicroVideo" in header ||
            "MotionPhoto_Data" in header
    } ?: false

    private fun copyUriToFile(uri: Uri, target: File) {
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
                output.flush()
                output.fd.sync()
            }
        } ?: throw IOException("Unable to open source media")
        if (target.length() <= 0L) throw IOException("Source media is empty")
    }

    private fun writeFileToUri(source: File, uri: Uri) {
        resolver.update(
            uri,
            ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 1) },
            null,
            null
        )
        resolver.openOutputStream(uri, "rwt")?.use { output ->
            FileInputStream(source).use { it.copyTo(output) }
            output.flush()
            (output as? FileOutputStream)?.fd?.sync()
        } ?: throw IOException("Unable to open media for replacement")
    }

    private fun restore(
        backup: File,
        uri: Uri,
        dateModified: Long?,
        dateTaken: Long?
    ): Boolean {
        if (!backup.isFile || backup.length() <= 0L) return false
        return runCatching {
            writeFileToUri(backup, uri)
            resolver.update(
                uri,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                    dateModified?.let { put(MediaStore.MediaColumns.DATE_MODIFIED, it) }
                    dateTaken?.let { put(MediaStore.MediaColumns.DATE_TAKEN, it) }
                },
                null,
                null
            )
            sha256(uri) == sha256(backup)
        }.getOrDefault(false)
    }

    private fun writeJournal(
        journal: File,
        uri: Uri,
        backup: File,
        candidate: File,
        backupHash: String,
        dateModified: Long,
        dateTaken: Long?,
        phase: String
    ) {
        Properties().apply {
            setProperty("uri", uri.toString())
            setProperty("backup", backup.absolutePath)
            setProperty("candidate", candidate.absolutePath)
            setProperty("backupSha256", backupHash)
            setProperty("dateModified", dateModified.toString())
            dateTaken?.let { setProperty("dateTaken", it.toString()) }
            setProperty("phase", phase)
        }.storeTo(journal)
    }

    private fun updateJournalPhase(journal: File, phase: String) {
        val properties = journal.readProperties().apply { setProperty("phase", phase) }
        properties.storeTo(journal)
    }

    private fun Properties.storeTo(file: File) {
        FileOutputStream(file).use {
            store(it, null)
            it.fd.sync()
        }
    }

    private fun File.readProperties(): Properties = Properties().also { properties ->
        inputStream().use(properties::load)
    }

    private fun sha256(file: File): String = file.inputStream().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256(uri: Uri): String = resolver.openInputStream(uri)?.use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    } ?: throw IOException("Unable to verify media")

    private companion object {
        const val MINIMUM_FREE_MARGIN = 16L * 1024L * 1024L
        const val MOTION_PHOTO_SNIFF_BYTES = 1024 * 1024
    }
}
