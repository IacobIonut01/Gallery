/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.dao.CloudServerConfigDao
import com.dot.gallery.cloud.data.entity.CloudServerConfigEntity
import com.dot.gallery.core.Resource
import com.dot.gallery.core.activeDataStore
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import com.dot.gallery.feature_node.data.data_source.KeychainHolder
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.presentation.util.printError
import com.dot.gallery.feature_node.presentation.util.printInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exports and imports a complete ReFra configuration backup as a ZIP archive.
 *
 * The archive contains a [BackupManifest] (`manifest.json`) plus the fully decrypted
 * binary content of every vault media item under `vaults/<uuid>/<mediaId>`.
 *
 * Local (MediaStore) favorites cannot be re-applied without a system consent dialog,
 * so [importBackup] returns the list of matched device URIs that the caller must pass
 * to a `MediaStore.createFavoriteRequest` consent flow.
 */
@Singleton
class ConfigBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: InternalDatabase,
    private val keychainHolder: KeychainHolder,
    private val cloudMediaDao: CloudMediaDao,
    private val cloudServerConfigDao: CloudServerConfigDao,
    private val mediaRepository: MediaRepository
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    data class ExportResult(
        val settingsCount: Int,
        val localFavoritesCount: Int,
        val cloudFavoritesCount: Int,
        val cloudConfigsCount: Int,
        val vaultCount: Int,
        val vaultMediaCount: Int
    )

    data class ImportResult(
        val settingsRestored: Int,
        val cloudFavoritesRestored: Int,
        val cloudConfigsRestored: Int,
        val vaultsRestored: Int,
        val vaultMediaRestored: Int,
        /** Device media URIs that match backed-up local favorites and need a system consent to favorite. */
        val pendingLocalFavoriteUris: List<Uri>
    )

    // ---------------------------------------------------------------------------------------------
    // Export
    // ---------------------------------------------------------------------------------------------

    /**
     * Exports a backup to [destination]. When [password] is non-null, the resulting
     * ZIP is encrypted with [BackupCrypto] (password-based AES-256-GCM).
     */
    suspend fun exportBackup(
        destination: Uri,
        selection: BackupSelection = BackupSelection(),
        password: String? = null,
        onProgress: BackupProgressListener = { _, _, _ -> }
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        var tempZip: File? = null
        try {
            val result: ExportResult
            if (password.isNullOrEmpty()) {
                val output = context.contentResolver.openOutputStream(destination)
                    ?: return@withContext Result.failure(IOException("Unable to open output stream"))
                output.use { result = buildArchive(it, selection, onProgress) }
            } else {
                // Build the plaintext ZIP into a temp file, then stream-encrypt into the destination.
                tempZip = File(context.cacheDir, "backup_export_${System.currentTimeMillis()}.zip")
                result = tempZip.outputStream().use { buildArchive(it, selection, onProgress) }
                val output = context.contentResolver.openOutputStream(destination)
                    ?: return@withContext Result.failure(IOException("Unable to open output stream"))
                output.use { out ->
                    tempZip.inputStream().use { input ->
                        BackupCrypto.encrypt(input, out, password)
                    }
                }
            }
            Result.success(result)
        } catch (e: Exception) {
            printError("ConfigBackupManager: export failed: ${e.message}")
            Result.failure(e)
        } finally {
            tempZip?.delete()
        }
    }

    private data class PlannedVault(
        val vault: Vault,
        val media: List<Pair<Media.EncryptedMedia2, VaultMediaEntry>>
    )

    private suspend fun buildArchive(
        output: OutputStream,
        selection: BackupSelection,
        onProgress: BackupProgressListener
    ): ExportResult {
        val settings = if (selection.settings) {
            exportSettings().also { onProgress(BackupSection.SETTINGS, it.size, it.size) }
        } else emptyMap()
        val localFavorites = if (selection.localFavorites) {
            exportLocalFavorites().also { onProgress(BackupSection.LOCAL_FAVORITES, it.size, it.size) }
        } else emptyList()
        val cloudFavorites = if (selection.cloudFavorites) {
            exportCloudFavorites().also { onProgress(BackupSection.CLOUD_FAVORITES, it.size, it.size) }
        } else emptyList()
        val cloudConfigs = if (selection.cloudConfigs) {
            exportCloudConfigs().also { onProgress(BackupSection.CLOUD_CONFIGS, it.size, it.size) }
        } else emptyList()

        val planned = if (selection.vaults) planVaults(selection) else emptyList()
        val vaultEntries = planned.map { pv ->
            VaultEntry(pv.vault.uuid.toString(), pv.vault.name, pv.media.map { it.second })
        }
        val totalVaultMedia = vaultEntries.sumOf { it.media.size }
        if (selection.vaults) onProgress(BackupSection.VAULTS, 0, totalVaultMedia)

        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            // Write the manifest FIRST so a backup can be inspected by reading only the leading bytes.
            val manifest = BackupManifest(
                appVersionName = appVersionName(),
                appVersionCode = appVersionCode(),
                exportedAt = System.currentTimeMillis(),
                settings = settings,
                localFavorites = localFavorites,
                cloudFavorites = cloudFavorites,
                cloudConfigs = cloudConfigs,
                vaults = vaultEntries
            )
            zip.putNextEntry(ZipEntry(BackupManifest.MANIFEST_NAME))
            zip.write(json.encodeToString(BackupManifest.serializer(), manifest).toByteArray())
            zip.closeEntry()

            // Then stream the decrypted vault binaries, reporting per-item progress.
            var done = 0
            planned.forEach { pv ->
                pv.media.forEach { (media, entry) ->
                    zip.putNextEntry(ZipEntry(entry.fileName))
                    try {
                        writeDecryptedVaultMedia(pv.vault, media, zip)
                    } finally {
                        zip.closeEntry()
                    }
                    done++
                    onProgress(BackupSection.VAULTS, done, totalVaultMedia)
                }
            }
        }

        return ExportResult(
            settingsCount = settings.size,
            localFavoritesCount = localFavorites.size,
            cloudFavoritesCount = cloudFavorites.size,
            cloudConfigsCount = cloudConfigs.size,
            vaultCount = vaultEntries.size,
            vaultMediaCount = totalVaultMedia
        )
    }

    private suspend fun exportSettings(): Map<String, SettingValue> {
        val prefs = context.activeDataStore.data.first()
        return prefs.asMap().mapNotNull { (key, value) ->
            val sv = when (value) {
                is Boolean -> SettingValue("b", value.toString())
                is Int -> SettingValue("i", value.toString())
                is Long -> SettingValue("l", value.toString())
                is Float -> SettingValue("f", value.toString())
                is Double -> SettingValue("d", value.toString())
                is String -> SettingValue("s", value)
                is Set<*> -> SettingValue(
                    "ss",
                    json.encodeToString(
                        ListSerializer(String.serializer()),
                        value.map { it.toString() }
                    )
                )
                else -> null
            }
            sv?.let { key.name to it }
        }.toMap()
    }

    private suspend fun exportLocalFavorites(): List<LocalFavoriteEntry> {
        val favorites = mediaRepository.getFavorites(MediaOrder.Default)
            .first { it is Resource.Success || it is Resource.Error }
            .data.orEmpty()
        return favorites.map { media ->
            LocalFavoriteEntry(
                displayName = media.label,
                relativePath = media.relativePath,
                path = media.path,
                size = media.size,
                timestamp = media.timestamp
            )
        }
    }

    private suspend fun exportCloudFavorites(): List<CloudFavoriteEntry> {
        return cloudMediaDao.getFavoritesAsync().map { entity ->
            CloudFavoriteEntry(
                providerType = entity.providerType.name,
                remoteId = entity.remoteId
            )
        }
    }

    private suspend fun exportCloudConfigs(): List<CloudConfigEntry> {
        return cloudServerConfigDao.getAll().first().map { it.toEntry() }
    }

    /** Gathers vault media metadata (no binary writes) so the manifest can be produced up front. */
    private suspend fun planVaults(selection: BackupSelection): List<PlannedVault> {
        val vaults = database.getVaultDao().getVaults().first()
            .filter { selection.isVaultSelected(it.uuid.toString()) }
        return vaults.map { vault ->
            val mediaList = database.getVaultDao().getMediaFromVault(vault.uuid).first()
            val pairs = mediaList.mapNotNull { media ->
                val encFile = with(keychainHolder) { vault.mediaFile(media.id) }
                if (!encFile.exists()) {
                    printError("ConfigBackupManager: missing vault file for media ${media.id}")
                    return@mapNotNull null
                }
                val entryName = "${BackupManifest.VAULTS_DIR}/${vault.uuid}/${media.id}"
                media to media.toEntry(entryName)
            }
            PlannedVault(vault, pairs)
        }
    }

    /** Decrypts a single vault media item into [zip] (already positioned on its entry). */
    private fun writeDecryptedVaultMedia(
        vault: Vault,
        media: Media.EncryptedMedia2,
        zip: ZipOutputStream
    ) {
        val encFile = with(keychainHolder) { vault.mediaFile(media.id) }
        if (!encFile.exists()) {
            printError("ConfigBackupManager: missing vault file for media ${media.id}")
            return
        }
        if (keychainHolder.isPortableFile(encFile)) {
            keychainHolder.decryptPortableStream(vault, encFile, zip)
        } else {
            val decrypted = keychainHolder.decryptVaultMedia(encFile)
            val bytes = decrypted.bytes
            val tempFile = decrypted.tempFile
            try {
                when {
                    bytes != null -> zip.write(bytes)
                    tempFile != null -> tempFile.inputStream().use { it.copyTo(zip) }
                }
            } finally {
                // Always remove the transient decrypted file, even if the write fails.
                tempFile?.delete()
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Import
    // ---------------------------------------------------------------------------------------------

    /** Returns true if [source] points to a password-encrypted backup. */
    suspend fun isEncryptedBackup(source: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(source)?.use { input ->
                BackupCrypto.isEncrypted(BackupCrypto.readHeader(input))
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /** Sentinel used to stop decryption early once the manifest has been recovered. */
    private class ManifestFound(val manifest: BackupManifest) : RuntimeException()

    /**
     * Reads only the manifest of [source] (without restoring anything) and summarizes its
     * contents. For encrypted backups [password] is required; a wrong password yields a
     * [BackupPasswordException] failure. The manifest is the first archive entry, so for
     * encrypted backups only the leading chunk(s) are decrypted.
     */
    suspend fun inspectBackup(
        source: Uri,
        password: String? = null
    ): Result<BackupContents> = withContext(Dispatchers.IO) {
        try {
            val encrypted = isEncryptedBackup(source)
            val manifest: BackupManifest = if (!encrypted) {
                context.contentResolver.openInputStream(source)?.use { input ->
                    readManifestFromZipStream(input)
                } ?: return@withContext Result.failure(IOException("Unable to open input stream"))
            } else {
                if (password.isNullOrEmpty()) {
                    return@withContext Result.failure(BackupPasswordException())
                }
                decryptManifestOnly(source, password)
            } ?: return@withContext Result.failure(IOException("Invalid backup: manifest.json missing"))

            Result.success(
                BackupContents(
                    schemaVersion = manifest.schemaVersion,
                    appVersionName = manifest.appVersionName,
                    exportedAt = manifest.exportedAt,
                    encrypted = encrypted,
                    settingsCount = manifest.settings.size,
                    localFavoritesCount = manifest.localFavorites.size,
                    cloudFavoritesCount = manifest.cloudFavorites.size,
                    cloudConfigsCount = manifest.cloudConfigs.size,
                    vaultCount = manifest.vaults.size,
                    vaultMediaCount = manifest.vaults.sumOf { it.media.size },
                    vaults = manifest.vaults.map {
                        VaultSummary(it.uuid, it.name, it.media.size)
                    }
                )
            )
        } catch (e: BackupPasswordException) {
            Result.failure(e)
        } catch (e: Exception) {
            printError("ConfigBackupManager: inspect failed: ${e.message}")
            Result.failure(e)
        }
    }

    /** Reads `manifest.json` from a (plaintext) ZIP stream without extracting other entries. */
    private fun readManifestFromZipStream(input: InputStream): BackupManifest? {
        ZipInputStream(BufferedInputStream(input)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name == BackupManifest.MANIFEST_NAME) {
                    val text = zis.readBytes().decodeToString()
                    return json.decodeFromString(BackupManifest.serializer(), text)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return null
    }

    /** Decrypts just enough of an encrypted backup to recover its manifest (the first entry). */
    private fun decryptManifestOnly(source: Uri, password: String): BackupManifest? {
        val accumulated = ByteArrayOutputStream()
        var found: BackupManifest? = null
        val sink = object : OutputStream() {
            override fun write(b: Int) { accumulated.write(b) }
            override fun write(b: ByteArray, off: Int, len: Int) {
                accumulated.write(b, off, len)
                val manifest = runCatching {
                    readManifestFromZipStream(ByteArrayInputStream(accumulated.toByteArray()))
                }.getOrNull()
                if (manifest != null) throw ManifestFound(manifest)
            }
        }
        try {
            context.contentResolver.openInputStream(source)?.use { input ->
                BackupCrypto.decrypt(input, sink, password)
            }
        } catch (e: ManifestFound) {
            found = e.manifest
        }
        return found ?: runCatching {
            readManifestFromZipStream(ByteArrayInputStream(accumulated.toByteArray()))
        }.getOrNull()
    }

    /**
     * Imports a backup from [source]. When the backup is password-encrypted, [password]
     * must be supplied; an incorrect password yields a [BackupPasswordException] failure.
     */
    suspend fun importBackup(
        source: Uri,
        selection: BackupSelection = BackupSelection(),
        password: String? = null,
        onProgress: BackupProgressListener = { _, _, _ -> }
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        val tmpDir = File(context.cacheDir, "backup_import_${System.currentTimeMillis()}")
        val dataDir = File(tmpDir, "data")
        dataDir.mkdirs()
        try {
            val encrypted = isEncryptedBackup(source)
            if (encrypted) {
                if (password.isNullOrEmpty()) {
                    return@withContext Result.failure(BackupPasswordException())
                }
                val decryptedZip = File(tmpDir, "backup.zip")
                context.contentResolver.openInputStream(source)?.use { input ->
                    decryptedZip.outputStream().use { out ->
                        BackupCrypto.decrypt(input, out, password)
                    }
                } ?: return@withContext Result.failure(IOException("Unable to open input stream"))
                decryptedZip.inputStream().use { extractZipStream(it, dataDir) }
                decryptedZip.delete()
            } else {
                context.contentResolver.openInputStream(source)?.use { input ->
                    extractZipStream(input, dataDir)
                } ?: return@withContext Result.failure(IOException("Unable to open input stream"))
            }

            val manifestFile = File(dataDir, BackupManifest.MANIFEST_NAME)
            if (!manifestFile.exists()) {
                return@withContext Result.failure(IOException("Invalid backup: manifest.json missing"))
            }
            val manifest = json.decodeFromString(BackupManifest.serializer(), manifestFile.readText())

            var settingsRestored = 0
            var cloudFavoritesRestored = 0
            var cloudConfigsRestored = 0
            var vaultsRestored = 0
            var vaultMediaRestored = 0
            var pendingLocalFavoriteUris: List<Uri> = emptyList()

            if (selection.settings) {
                settingsRestored = restoreSettings(manifest)
                onProgress(BackupSection.SETTINGS, settingsRestored, manifest.settings.size)
            }
            if (selection.cloudConfigs) {
                cloudConfigsRestored = restoreCloudConfigs(manifest)
                onProgress(BackupSection.CLOUD_CONFIGS, cloudConfigsRestored, manifest.cloudConfigs.size)
            }
            if (selection.cloudFavorites) {
                cloudFavoritesRestored = restoreCloudFavorites(manifest)
                onProgress(BackupSection.CLOUD_FAVORITES, cloudFavoritesRestored, manifest.cloudFavorites.size)
            }
            if (selection.vaults) {
                val selectedVaults = manifest.vaults.filter {
                    selection.isVaultSelected(it.uuid)
                }
                val total = selectedVaults.sumOf { it.media.size }
                onProgress(BackupSection.VAULTS, 0, total)
                val (vaults, mediaCount) = restoreVaults(selectedVaults, dataDir) { done ->
                    onProgress(BackupSection.VAULTS, done, total)
                }
                vaultsRestored = vaults
                vaultMediaRestored = mediaCount
            }
            if (selection.localFavorites) {
                pendingLocalFavoriteUris = matchLocalFavorites(manifest)
                onProgress(
                    BackupSection.LOCAL_FAVORITES,
                    pendingLocalFavoriteUris.size,
                    manifest.localFavorites.size
                )
            }

            Result.success(
                ImportResult(
                    settingsRestored = settingsRestored,
                    cloudFavoritesRestored = cloudFavoritesRestored,
                    cloudConfigsRestored = cloudConfigsRestored,
                    vaultsRestored = vaultsRestored,
                    vaultMediaRestored = vaultMediaRestored,
                    pendingLocalFavoriteUris = pendingLocalFavoriteUris
                )
            )
        } catch (e: Exception) {
            printError("ConfigBackupManager: import failed: ${e.message}")
            Result.failure(e)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    private fun extractZipStream(input: java.io.InputStream, destDir: File) {
        ZipInputStream(BufferedInputStream(input)).use { zis ->
            val destCanonical = destDir.canonicalPath
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                // Zip-slip protection
                if (!outFile.canonicalPath.startsWith(destCanonical + File.separator) &&
                    outFile.canonicalPath != destCanonical
                ) {
                    throw IOException("Illegal zip entry path: ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private suspend fun restoreSettings(manifest: BackupManifest): Int {
        if (manifest.settings.isEmpty()) return 0
        context.activeDataStore.edit { prefs ->
            manifest.settings.forEach { (name, sv) -> applySetting(prefs, name, sv) }
        }
        return manifest.settings.size
    }

    private fun applySetting(prefs: MutablePreferences, name: String, sv: SettingValue) {
        try {
            when (sv.type) {
                "b" -> prefs[booleanPreferencesKey(name)] = sv.value.toBoolean()
                "i" -> prefs[intPreferencesKey(name)] = sv.value.toInt()
                "l" -> prefs[longPreferencesKey(name)] = sv.value.toLong()
                "f" -> prefs[floatPreferencesKey(name)] = sv.value.toFloat()
                "d" -> prefs[doublePreferencesKey(name)] = sv.value.toDouble()
                "s" -> prefs[stringPreferencesKey(name)] = sv.value
                "ss" -> prefs[stringSetPreferencesKey(name)] = json.decodeFromString(
                    ListSerializer(String.serializer()),
                    sv.value
                ).toSet()
            }
        } catch (e: Exception) {
            printError("ConfigBackupManager: skipping setting '$name': ${e.message}")
        }
    }

    private suspend fun restoreCloudConfigs(manifest: BackupManifest): Int {
        var count = 0
        manifest.cloudConfigs.forEach { entry ->
            val providerType = runCatching { ProviderType.valueOf(entry.providerType) }.getOrNull()
                ?: return@forEach
            cloudServerConfigDao.insert(entry.toEntity(providerType))
            count++
        }
        return count
    }

    private suspend fun restoreCloudFavorites(manifest: BackupManifest): Int {
        var count = 0
        manifest.cloudFavorites.forEach { fav ->
            val providerType = runCatching { ProviderType.valueOf(fav.providerType) }.getOrNull()
                ?: return@forEach
            cloudMediaDao.updateFavorite(fav.remoteId, providerType, true)
            count++
        }
        return count
    }

    private suspend fun restoreVaults(
        vaults: List<VaultEntry>,
        tmpDir: File,
        onMediaRestored: (done: Int) -> Unit = {}
    ): Pair<Int, Int> {
        var vaultCount = 0
        var mediaCount = 0
        vaults.forEach { vaultEntry ->
            val uuid = runCatching { UUID.fromString(vaultEntry.uuid) }.getOrNull() ?: return@forEach
            val vault = Vault(uuid = uuid, name = vaultEntry.name)
            // Always create as a portable (transferable) vault so the imported binaries can be re-encrypted.
            keychainHolder.writeVaultInfo(vault, transferable = true)
            database.getVaultDao().insertVault(vault)
            vaultCount++

            vaultEntry.media.forEach { mediaEntry ->
                val srcFile = File(tmpDir, mediaEntry.fileName)
                if (!srcFile.exists()) {
                    printError("ConfigBackupManager: missing media binary ${mediaEntry.fileName}")
                    return@forEach
                }
                val outFile = with(keychainHolder) { vault.mediaFile(mediaEntry.id) }
                if (outFile.exists()) outFile.delete()
                try {
                    srcFile.inputStream().use { input ->
                        keychainHolder.encryptPortableStream(vault, input, outFile)
                    }
                    outFile.setLastModified(System.currentTimeMillis())
                    database.getVaultDao().addMediaToVault(mediaEntry.toEncryptedMedia2(uuid))
                    mediaCount++
                    onMediaRestored(mediaCount)
                } catch (e: Exception) {
                    printError("ConfigBackupManager: failed to import vault media ${mediaEntry.id}: ${e.message}")
                    outFile.delete()
                }
            }
        }
        return vaultCount to mediaCount
    }

    private suspend fun matchLocalFavorites(manifest: BackupManifest): List<Uri> {
        if (manifest.localFavorites.isEmpty()) return emptyList()
        val deviceMedia = mediaRepository.getMedia()
            .first { it is Resource.Success || it is Resource.Error }
            .data.orEmpty()
        if (deviceMedia.isEmpty()) return emptyList()

        val byPathName = deviceMedia.associateBy { it.relativePath + "/" + it.label }
        val result = LinkedHashSet<Uri>()
        manifest.localFavorites.forEach { fav ->
            val match = byPathName[fav.relativePath + "/" + fav.displayName]
                ?: deviceMedia.firstOrNull { it.label == fav.displayName && it.size == fav.size }
            if (match != null) result.add(match.uri)
        }
        printInfo("ConfigBackupManager: matched ${result.size}/${manifest.localFavorites.size} local favorites")
        return result.toList()
    }

    // ---------------------------------------------------------------------------------------------
    // Mappers / helpers
    // ---------------------------------------------------------------------------------------------

    private fun CloudServerConfigEntity.toEntry() = CloudConfigEntry(
        providerType = providerType.name,
        serverUrl = serverUrl,
        apiKey = apiKey,
        username = username,
        encryptedPassword = encryptedPassword,
        displayName = displayName,
        isActive = isActive,
        lastConnected = lastConnected,
        syncEnabled = syncEnabled,
        wifiOnly = wifiOnly,
        syncIntervalMinutes = syncIntervalMinutes,
        syncFolders = syncFolders,
        cellularPhotos = cellularPhotos,
        cellularVideos = cellularVideos,
        requireCharging = requireCharging,
        syncAlbums = syncAlbums,
        showBackupTotalProgress = showBackupTotalProgress,
        showBackupDetailProgress = showBackupDetailProgress,
        notifyBackupFailures = notifyBackupFailures,
        autoUrlSwitch = autoUrlSwitch,
        localWifiSsid = localWifiSsid,
        localServerUrl = localServerUrl,
        externalUrls = externalUrls,
        loadPreviewImage = loadPreviewImage,
        loadOriginalImage = loadOriginalImage,
        autoPlayVideos = autoPlayVideos,
        loopVideos = loopVideos,
        forceOriginalVideo = forceOriginalVideo,
        verboseLogging = verboseLogging,
        syncRemoteDeletions = syncRemoteDeletions,
        preferRemoteImages = preferRemoteImages,
        readOnlyMode = readOnlyMode
    )

    private fun CloudConfigEntry.toEntity(providerType: ProviderType) = CloudServerConfigEntity(
        id = 0L,
        providerType = providerType,
        serverUrl = serverUrl,
        apiKey = apiKey,
        username = username,
        encryptedPassword = encryptedPassword,
        displayName = displayName,
        isActive = isActive,
        lastConnected = lastConnected,
        syncEnabled = syncEnabled,
        wifiOnly = wifiOnly,
        syncIntervalMinutes = syncIntervalMinutes,
        syncFolders = syncFolders,
        cellularPhotos = cellularPhotos,
        cellularVideos = cellularVideos,
        requireCharging = requireCharging,
        syncAlbums = syncAlbums,
        showBackupTotalProgress = showBackupTotalProgress,
        showBackupDetailProgress = showBackupDetailProgress,
        notifyBackupFailures = notifyBackupFailures,
        autoUrlSwitch = autoUrlSwitch,
        localWifiSsid = localWifiSsid,
        localServerUrl = localServerUrl,
        externalUrls = externalUrls,
        loadPreviewImage = loadPreviewImage,
        loadOriginalImage = loadOriginalImage,
        autoPlayVideos = autoPlayVideos,
        loopVideos = loopVideos,
        forceOriginalVideo = forceOriginalVideo,
        verboseLogging = verboseLogging,
        syncRemoteDeletions = syncRemoteDeletions,
        preferRemoteImages = preferRemoteImages,
        readOnlyMode = readOnlyMode
    )

    private fun Media.EncryptedMedia2.toEntry(fileName: String) = VaultMediaEntry(
        id = id,
        label = label,
        path = path,
        relativePath = relativePath,
        albumID = albumID,
        albumLabel = albumLabel,
        timestamp = timestamp,
        expiryTimestamp = expiryTimestamp,
        takenTimestamp = takenTimestamp,
        fullDate = fullDate,
        mimeType = mimeType,
        favorite = favorite,
        trashed = trashed,
        size = size,
        duration = duration,
        fileName = fileName
    )

    private fun VaultMediaEntry.toEncryptedMedia2(uuid: UUID) = Media.EncryptedMedia2(
        id = id,
        label = label,
        uuid = uuid,
        path = path,
        relativePath = relativePath,
        albumID = albumID,
        albumLabel = albumLabel,
        timestamp = timestamp,
        expiryTimestamp = expiryTimestamp,
        takenTimestamp = takenTimestamp,
        fullDate = fullDate,
        mimeType = mimeType,
        favorite = favorite,
        trashed = trashed,
        size = size,
        duration = duration
    )

    private fun appVersionName(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (_: Exception) {
        ""
    }

    @Suppress("DEPRECATION")
    private fun appVersionCode(): Long = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }
    } catch (_: Exception) {
        0L
    }
}
