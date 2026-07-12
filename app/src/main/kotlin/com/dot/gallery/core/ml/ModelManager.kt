/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ml

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.dot.gallery.BuildConfig
import com.dot.gallery.feature_node.presentation.util.printDebug
import com.dot.gallery.feature_node.presentation.util.printInfo
import com.dot.gallery.feature_node.presentation.util.printWarning
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadInfo(
    val speed: Long = 0L,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val currentFile: String = ""
)

data class ModelFileInfo(
    val name: String,
    val size: Long,
    val sha256: String,
    val verified: Boolean
)

enum class ModelStatus {
    NOT_INSTALLED,
    COPYING,
    DOWNLOADING,
    READY,
    ERROR
}

/**
 * A downloadable/bundled set of ML model files, grouped by the feature it powers so each feature
 * can be installed, checked and removed independently.
 *  - [SEARCH]: CLIP models for smart search + automatic categories.
 *  - [CUTOUT]: MobileSAM models for the subject-cutout feature in the media viewer.
 */
enum class ModelGroup(val subDir: String, val files: List<String>) {
    SEARCH(
        subDir = "clip",
        files = listOf("visual_quant.onnx", "textual_quant.onnx", "vocab.json", "merges.txt")
    ),
    CUTOUT(
        subDir = "sam",
        files = listOf("mobile_sam_image_encoder.onnx", "sam_mask_decoder_single.onnx")
    );

    companion object {
        /** The group a model file belongs to (defaults to [SEARCH] for unknown names). */
        fun of(fileName: String): ModelGroup = entries.firstOrNull { fileName in it.files } ?: SEARCH
    }
}

@Singleton
class ModelManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    // Per-group observable state. Each ModelGroup tracks its own status, progress and errors so the
    // two feature sets (search/categories vs. subject cutout) install and uninstall independently.
    private class GroupFlows {
        val status = MutableStateFlow(ModelStatus.NOT_INSTALLED)
        val progress = MutableStateFlow(0f)
        val error = MutableStateFlow<String?>(null)
        val info = MutableStateFlow(DownloadInfo())
    }

    private val flows: Map<ModelGroup, GroupFlows> = ModelGroup.entries.associateWith { GroupFlows() }
    private fun flows(group: ModelGroup) = flows.getValue(group)

    fun status(group: ModelGroup): StateFlow<ModelStatus> = flows(group).status.asStateFlow()
    fun downloadProgress(group: ModelGroup): StateFlow<Float> = flows(group).progress.asStateFlow()
    fun errorMessage(group: ModelGroup): StateFlow<String?> = flows(group).error.asStateFlow()
    fun downloadInfo(group: ModelGroup): StateFlow<DownloadInfo> = flows(group).info.asStateFlow()

    private val mutex = Mutex()

    fun isReady(group: ModelGroup): Boolean = flows(group).status.value == ModelStatus.READY

    /** True only when every group's models are present. */
    val isReady: Boolean get() = ModelGroup.entries.all { isReady(it) }

    /**
     * Whether the app has INTERNET permission declared in its manifest.
     * When false, model *download* is not possible, but bundled models still work.
     */
    val hasInternetPermission: Boolean by lazy {
        context.packageManager.checkPermission(
            Manifest.permission.INTERNET,
            context.packageName
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Whether AI features (categories, smart search, etc.) should be shown in the UI.
     * True when models are bundled (withML builds) OR the app can download them (has INTERNET).
     * This decouples AI feature visibility from INTERNET permission so that
     * offline-withML builds (which strip INTERNET but bundle models) still expose AI features.
     */
    val areAiFeaturesAvailable: Boolean by lazy {
        BuildConfig.ML_MODELS_BUNDLED || hasInternetPermission
    }

    val modelsDir: File get() = File(context.filesDir, MODELS_DIR)

    fun getDestinationFile(name: String): File =
        File(modelsDir, "${ModelGroup.of(name).subDir}/$name")

    fun getTempFile(name: String): File =
        File(modelsDir, "${ModelGroup.of(name).subDir}/$name.tmp")

    /**
     * Initialize models on app start.
     * For withML builds: copies bundled assets to filesDir if not already present.
     * For noML builds: checks if models have been previously downloaded.
     */
    suspend fun initializeModels() = mutex.withLock {
        withContext(Dispatchers.IO) {
            ModelGroup.entries.forEach { group -> initializeGroup(group) }
        }
    }

    private fun initializeGroup(group: ModelGroup) {
        if (checkModelsPresent(group)) {
            flows(group).status.value = ModelStatus.READY
            printInfo("ModelManager: ${group.name} models already present in filesDir")
            return
        }
        if (BuildConfig.ML_MODELS_BUNDLED) {
            copyBundledModels(group)
            if (checkModelsPresent(group)) {
                flows(group).status.value = ModelStatus.READY
            } else {
                flows(group).status.value = ModelStatus.NOT_INSTALLED
                printInfo("ModelManager: ${group.name} bundled copy pass completed, but some files are missing (will download on-demand)")
            }
        } else {
            flows(group).status.value = ModelStatus.NOT_INSTALLED
            printInfo("ModelManager: ${group.name} models not installed (noML build)")
        }
    }

    /**
     * Check if all model files for [group] are present and non-empty.
     */
    fun checkModelsPresent(group: ModelGroup): Boolean {
        return group.files.all { fileName ->
            val file = getDestinationFile(fileName)
            file.exists() && file.length() > 0
        }
    }

    /**
     * Get a model file by name.
     * @throws ModelsNotAvailableException if that file's group is not installed.
     */
    fun getModelFile(name: String): File {
        val group = ModelGroup.of(name)
        if (!isReady(group)) throw ModelsNotAvailableException()
        val file = getDestinationFile(name)
        if (!file.exists()) throw ModelsNotAvailableException("Model file not found: $name")
        return file
    }

    /**
     * Get installed model size in bytes for [group].
     */
    fun getInstalledSize(group: ModelGroup): Long {
        if (!checkModelsPresent(group)) return 0L
        return group.files.sumOf { getDestinationFile(it).length() }
    }

    /**
     * Get detailed info (name, size, SHA-256) for each installed file in [group].
     */
    fun getFileInfos(group: ModelGroup): List<ModelFileInfo> {
        if (!checkModelsPresent(group)) return emptyList()
        return group.files.map { fileName ->
            val file = getDestinationFile(fileName)
            val hash = file.sha256()
            ModelFileInfo(
                name = fileName,
                size = file.length(),
                sha256 = hash,
                verified = EXPECTED_CHECKSUMS[fileName] == hash
            )
        }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { stream ->
            val buffer = ByteArray(65536)
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Delete all downloaded/copied model files.
     */
    suspend fun deleteModels(group: ModelGroup) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val dir = File(modelsDir, group.subDir)
            if (dir.exists()) {
                dir.deleteRecursively()
                printInfo("ModelManager: ${group.name} models deleted")
            }
            flows(group).apply {
                status.value = ModelStatus.NOT_INSTALLED
                progress.value = 0f
                error.value = null
                info.value = DownloadInfo()
            }
            Unit
        }
    }

    /**
     * Called by ModelDownloadWorker to update download progress for [group].
     */
    fun updateDownloadProgress(group: ModelGroup, progress: Float) {
        flows(group).progress.value = progress
        flows(group).status.value = ModelStatus.DOWNLOADING
    }

    fun updateDownloadInfo(group: ModelGroup, info: DownloadInfo) {
        flows(group).info.value = info
    }

    /**
     * Called by ModelDownloadWorker when a group's download completes successfully.
     */
    fun onDownloadComplete(group: ModelGroup) {
        if (checkModelsPresent(group)) {
            flows(group).status.value = ModelStatus.READY
            flows(group).progress.value = 100f
            flows(group).error.value = null
            printInfo("ModelManager: ${group.name} download complete, models ready")
        } else {
            flows(group).status.value = ModelStatus.ERROR
            flows(group).error.value = "Download completed but model files are missing"
            printWarning("ModelManager: ${group.name} download completed but validation failed")
        }
    }

    /**
     * Called by ModelDownloadWorker on failure for [group].
     */
    fun onDownloadFailed(group: ModelGroup, error: String) {
        flows(group).status.value = ModelStatus.ERROR
        flows(group).error.value = error
        flows(group).progress.value = 0f
        printWarning("ModelManager: ${group.name} download failed: $error")
    }

    /**
     * Copy a group's bundled assets to filesDir (withML builds only).
     */
    private fun copyBundledModels(group: ModelGroup) {
        flows(group).status.value = ModelStatus.COPYING
        try {
            modelsDir.mkdirs()
            val assetManager = context.assets
            val totalFiles = group.files.size
            group.files.forEachIndexed { index, fileName ->
                val destFile = getDestinationFile(fileName)
                destFile.parentFile?.mkdirs()
                if (!destFile.exists() || destFile.length() == 0L) {
                    try {
                        printDebug("ModelManager: Copying asset $fileName to filesDir")
                        assetManager.open(fileName).use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output, bufferSize = 65536)
                            }
                        }
                    } catch (e: java.io.FileNotFoundException) {
                        printWarning("ModelManager: Bundled asset $fileName not found in assets, skipping copy.")
                    }
                }
                flows(group).progress.value = ((index + 1).toFloat() / totalFiles) * 100f
            }
            printInfo("ModelManager: ${group.name} bundled models copy pass completed")
        } catch (e: Exception) {
            flows(group).status.value = ModelStatus.ERROR
            flows(group).error.value = "Failed to copy bundled models: ${e.message}"
            printWarning("ModelManager: Failed to copy bundled models: ${e.message}")
        }
    }

    companion object {
        const val MODELS_DIR = "models"

        /** Flattened list of every model file across all groups. */
        val REQUIRED_FILES: List<String> get() = ModelGroup.entries.flatMap { it.files }

        const val BASE_DOWNLOAD_URL =
            "https://raw.githubusercontent.com/IacobIonut01/ReFra/refs/heads/main/ml-models/src/main/assets/"

        val EXPECTED_CHECKSUMS = mapOf(
            "visual_quant.onnx" to "a2fbb26b5f6ab5c79dd9bf99ab2dbac4711abc88dc2e20afc02a0827aa3d59c2",
            "textual_quant.onnx" to "1ebb71a5ea1897823a829af8fc8168c5cfff761969bb62aee1fafdf5a2788aba",
            "vocab.json" to "e089ad92ba36837a0d31433e555c8f45fe601ab5c221d4f607ded32d9f7a4349",
            "merges.txt" to "9fd691f7c8039210e0fced15865466c65820d09b63988b0174bfe25de299051a",
            "mobile_sam_image_encoder.onnx" to "580f5fb648ea1062c0aabc26217aed56921985f03f0cbbd852bba81d760cc749",
            "sam_mask_decoder_single.onnx" to "93915fc7c993ab9d59ab8c9ccd3bce37f7509c81ab4150a74abd4d2abbd8570d"
        )
    }
}

class ModelsNotAvailableException(
    message: String = "ML models are not installed. Download them from Settings > Smart Features."
) : RuntimeException(message)
