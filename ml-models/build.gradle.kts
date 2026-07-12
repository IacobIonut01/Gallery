import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.io.File
import java.io.OutputStream
import java.security.MessageDigest

plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("ml_models")
    dynamicDelivery {
        deliveryType.set("install-time")
    }
}

// ---------------------------------------------------------------------------
// Generic large-model split / reassemble mechanism.
//
// GitHub rejects any file over 100 MB. Instead of committing oversized model
// assets, we split them into <100 MB parts (committed under `partsDir`), record
// a manifest, and reassemble the originals into their asset dir at build time.
// This is model-agnostic: to add a new large model, drop it in a source dir,
// run `./gradlew :ml-models:splitLargeModels`, and commit the generated parts +
// manifest + .gitignore diff.
//
//   splitLargeModels  – manual helper: detect oversized assets, split, manifest,
//                       delete the monolith, update .gitignore.
//   assembleModels    – automatic (pre-build): reassemble originals from parts.
//   checkModelSizes   – guard (build/CI): fail if an unmanaged asset is too big.
// ---------------------------------------------------------------------------

// --- Config (edit these to tune the mechanism) -----------------------------
val mlSourceDirs = listOf("src/main/assets")   // dirs scanned for oversized files
val mlPartsDir = "src/main/model-parts"        // committed parts + manifest.json
val mlMaxBytes = 100L * 1024 * 1024            // 100 MiB guard/detection threshold
val mlPartBytes = 90L * 1024 * 1024            // 90 MiB per part (safe margin)
val mlManifestName = "manifest.json"
val mlGitignoreBegin = "# >>> ml-models split (managed) — reassembled at build time, do not commit"
val mlGitignoreEnd = "# <<< ml-models split (managed)"

// --- Pure helpers (no Project references -> configuration-cache safe) -------
data class MlSplitModel(
    val asset: String,
    val sourceDir: String,
    val parts: List<String>,
    val sha256: String,
    val size: Long
)

// Wrapped in a top-level object (its own class, not a member of the build
// script) so the task actions below can call these without capturing the
// Project/script instance — required for configuration-cache compatibility.
object MlSplit {
    fun sha256(f: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        f.inputStream().buffered().use { stream ->
            val buffer = ByteArray(1 shl 16)
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Suppress("UNCHECKED_CAST")
    fun readManifest(manifestFile: File): List<MlSplitModel> {
        if (!manifestFile.exists()) return emptyList()
        val root = JsonSlurper().parseText(manifestFile.readText()) as Map<String, Any?>
        val models = root["models"] as? List<Map<String, Any?>> ?: emptyList()
        return models.map {
            MlSplitModel(
                asset = it["asset"] as String,
                sourceDir = it["sourceDir"] as String,
                parts = (it["parts"] as List<*>).map(Any?::toString),
                sha256 = it["sha256"] as String,
                size = (it["size"] as Number).toLong()
            )
        }
    }

    fun writeManifest(manifestFile: File, models: List<MlSplitModel>, partBytes: Long) {
        manifestFile.parentFile.mkdirs()
        val payload = mapOf(
            "partBytes" to partBytes,
            "models" to models.sortedBy { it.asset }.map {
                linkedMapOf(
                    "asset" to it.asset,
                    "sourceDir" to it.sourceDir,
                    "parts" to it.parts,
                    "sha256" to it.sha256,
                    "size" to it.size
                )
            }
        )
        manifestFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(payload)) + "\n")
    }

    fun splitFile(src: File, partsRoot: File, partBytes: Long): List<String> {
        partsRoot.mkdirs()
        // Remove any stale parts for this asset (e.g. if the model shrank).
        partsRoot.listFiles { _, name -> name.startsWith("${src.name}.part") }?.forEach { it.delete() }
        val names = mutableListOf<String>()
        src.inputStream().buffered().use { input ->
            val buf = ByteArray(1 shl 20)
            var idx = 0
            var written = 0L
            var out: OutputStream? = null
            fun openNew() {
                val name = "%s.part%03d".format(src.name, idx)
                names.add(name)
                out = File(partsRoot, name).outputStream().buffered()
                written = 0L
            }
            openNew()
            while (true) {
                val cap = (partBytes - written).coerceAtMost(buf.size.toLong()).toInt()
                if (cap == 0) { out!!.close(); idx++; openNew(); continue }
                val r = input.read(buf, 0, cap)
                if (r == -1) break
                out!!.write(buf, 0, r)
                written += r
            }
            out!!.close()
        }
        return names
    }

    fun rewriteGitignore(gitignore: File, entries: List<String>, begin: String, end: String) {
        val lines = if (gitignore.exists()) gitignore.readText().lines() else emptyList()
        val cleaned = mutableListOf<String>()
        var inBlock = false
        for (line in lines) {
            if (line.trim() == begin) { inBlock = true; continue }
            if (line.trim() == end) { inBlock = false; continue }
            if (!inBlock) cleaned.add(line)
        }
        while (cleaned.isNotEmpty() && cleaned.last().isBlank()) cleaned.removeAt(cleaned.size - 1)
        val out = cleaned.toMutableList()
        val sorted = entries.sorted()
        if (sorted.isNotEmpty()) {
            if (out.isNotEmpty()) out.add("")
            out.add(begin)
            out.addAll(sorted)
            out.add(end)
        }
        gitignore.writeText(out.joinToString("\n") + "\n")
    }
}

// Config-time File captures shared by the tasks below. Referencing Project
// (file(), rootProject, projectDir) only happens here at configuration time so
// the task actions stay configuration-cache compatible.
val mlProjectDir: File = projectDir
val mlRootDir: File = rootProject.projectDir
val mlPartsRoot = File(mlProjectDir, mlPartsDir)
val mlManifestFile = File(mlPartsRoot, mlManifestName)
val mlGitignoreFile = File(mlRootDir, ".gitignore")
val mlSourceDirFiles: List<Pair<String, File>> = mlSourceDirs.map { it to File(mlProjectDir, it) }

tasks.register("splitLargeModels") {
    group = "ml-models"
    description = "Detect assets over ${mlMaxBytes / 1024 / 1024} MiB and split them into committed parts."
    // Capture immutable config values for the action (config-cache safe).
    val sourceDirFiles = mlSourceDirFiles
    val partsRoot = mlPartsRoot
    val manifestFile = mlManifestFile
    val gitignoreFile = mlGitignoreFile
    val rootDir = mlRootDir
    val projectDirFile = mlProjectDir
    val maxBytes = mlMaxBytes
    val partBytes = mlPartBytes
    val begin = mlGitignoreBegin
    val end = mlGitignoreEnd
    doLast {
        val models = MlSplit.readManifest(manifestFile).associateBy { it.asset }.toMutableMap()
        var changed = false
        sourceDirFiles.forEach { (srcName, srcDir) ->
            srcDir.listFiles()
                ?.filter { it.isFile && it.length() > maxBytes }
                ?.sortedBy { it.name }
                ?.forEach { big ->
                    logger.lifecycle("splitLargeModels: splitting ${big.name} (${big.length()} bytes)…")
                    val partNames = MlSplit.splitFile(big, partsRoot, partBytes)
                    val sha = MlSplit.sha256(big)
                    models[big.name] = MlSplitModel(big.name, srcName, partNames, sha, big.length())
                    big.delete()
                    changed = true
                    logger.lifecycle("splitLargeModels: ${big.name} -> ${partNames.size} part(s); removed monolith from $srcName")
                }
        }
        if (changed) {
            val list = models.values.toList()
            MlSplit.writeManifest(manifestFile, list, partBytes)
            val entries = list.map { File(File(projectDirFile, it.sourceDir), it.asset).relativeTo(rootDir).path }
            MlSplit.rewriteGitignore(gitignoreFile, entries, begin, end)
            logger.lifecycle("splitLargeModels: updated $manifestFile and .gitignore. Commit the parts + manifest + .gitignore change.")
        } else {
            logger.lifecycle("splitLargeModels: no assets over the size limit; nothing to do.")
        }
    }
}

val assembleModels = tasks.register("assembleModels") {
    group = "ml-models"
    description = "Reassemble split model assets from committed parts (runs before packaging)."
    val partsRoot = mlPartsRoot
    val manifestFile = mlManifestFile
    val models = MlSplit.readManifest(manifestFile)
    if (manifestFile.exists()) inputs.file(manifestFile)
    if (partsRoot.exists()) inputs.dir(partsRoot)
    val targets = models.map { it to File(mlProjectDir, "${it.sourceDir}/${it.asset}") }
    outputs.files(*targets.map { it.second }.toTypedArray())
    doLast {
        targets.forEach { (m, target) ->
            val parts = m.parts.map { File(partsRoot, it) }.sortedBy { it.name }
            val missing = parts.filter { !it.exists() }
            if (missing.isNotEmpty()) {
                throw GradleException("assembleModels: missing parts for ${m.asset}: ${missing.joinToString { it.name }}")
            }
            val upToDate = target.exists() && target.length() == m.size && MlSplit.sha256(target) == m.sha256
            if (!upToDate) {
                target.parentFile.mkdirs()
                target.outputStream().buffered().use { out ->
                    parts.forEach { p -> p.inputStream().buffered().use { it.copyTo(out) } }
                }
                val sha = MlSplit.sha256(target)
                if (sha != m.sha256) {
                    target.delete()
                    throw GradleException("assembleModels: SHA-256 mismatch for ${m.asset}: expected ${m.sha256}, got $sha")
                }
                logger.lifecycle("assembleModels: reassembled ${m.asset} from ${parts.size} part(s)")
            }
        }
    }
}

tasks.register("checkModelSizes") {
    group = "verification"
    description = "Fail the build if an unmanaged asset exceeds the ${mlMaxBytes / 1024 / 1024} MiB limit."
    val sourceDirFiles = mlSourceDirFiles
    val manifestFile = mlManifestFile
    val maxBytes = mlMaxBytes
    doLast {
        val managed = MlSplit.readManifest(manifestFile).map { it.asset }.toSet()
        val offenders = sourceDirFiles
            .flatMap { it.second.listFiles()?.toList() ?: emptyList() }
            .filter { it.isFile && it.length() > maxBytes && it.name !in managed }
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "checkModelSizes: these assets exceed $maxBytes bytes and are not managed: " +
                    offenders.joinToString { "${it.name} (${it.length()} bytes)" } +
                    ". Run `./gradlew :ml-models:splitLargeModels`."
            )
        }
    }
}

// Ensure originals are reassembled before this module's assets/asset-pack tasks run.
tasks.configureEach {
    if ((name.contains("assets", ignoreCase = true) || name.contains("AssetPack", ignoreCase = true)) &&
        name != "assembleModels"
    ) {
        dependsOn(assembleModels)
    }
}
