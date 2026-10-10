/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs.capture

import android.util.Log
import com.dot.gallery.docs.DocsScene
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Accumulates per-scene capture results and emits `docs-manifest.json`
 * fragments (DOCS_SITE_PLAN.md §7):
 *
 *   docs-out/
 *     docs-manifest.json           merged index of every recorded scene
 *     manifest/<scene-id>.json     per-scene fragment (merge unit across runs)
 *     <scene-id>/<theme>/<device>-<state>.webp
 *
 * Fragments merge by (theme, device, state) so a partial rerun — dark only, or
 * one device — extends rather than replaces earlier captures; this run's
 * record wins for the same key. Records from a persisted fragment whose state
 * is no longer declared on the re-recorded scene are pruned; other scenes'
 * fragments pass through untouched.
 */
class DocsManifestWriter(private val outDir: File) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    /** scene id -> (scene metadata object, capture key -> capture object) */
    private val scenes = mutableMapOf<String, Pair<JsonObject, MutableMap<String, JsonObject>>>()

    private fun key(theme: String?, device: String, state: String?) =
        "${theme ?: "light"}\u0000${device}\u0000${state ?: "default"}"

    /**
     * Record one captured frame for [scene]. [file] is relative to [outDir].
     * [widthDp]/[heightDp] are the *rendered* logical dims — the preset dims
     * for full screens, the scene's own size for components — so a tablet
     * frame never reports the scene's phone defaults.
     */
    fun record(
        scene: DocsScene,
        device: String,
        state: String?,
        theme: String?,
        file: String,
        widthPx: Int,
        heightPx: Int,
        widthDp: Int,
        heightDp: Int,
    ) {
        val entry = scenes.getOrPut(scene.id) { sceneMeta(scene) to mutableMapOf() }
        entry.second[key(theme, device, state)] = buildJsonObject {
            put("device", device)
            put("state", state ?: "default")
            put("theme", theme ?: "light")
            put("file", file)
            put("width", widthPx)
            put("height", heightPx)
            put("widthDp", widthDp)
            put("heightDp", heightDp)
        }
    }

    /** Merge persisted fragments with this run's records and write everything back. */
    fun write(): File {
        val fragmentsDir = File(outDir, "manifest")
        fragmentsDir.mkdirs()
        loadExistingFragments(fragmentsDir)
        val manifest = File(outDir, "docs-manifest.json")
        val merged = buildJsonObject {
            put("schema", 1)
            put("generatedAtMs", System.currentTimeMillis())
            put("source", "docs-capture")
            putJsonArray("scenes") {
                scenes.toSortedMap().forEach { (id, entry) ->
                    add(sceneJson(id, entry))
                }
            }
        }
        scenes.toSortedMap().forEach { (id, entry) ->
            writeAtomic(File(fragmentsDir, "$id.json"), json.encodeToString(sceneJson(id, entry)))
        }
        writeAtomic(manifest, json.encodeToString(merged))
        Log.i(TAG, "wrote ${manifest.absolutePath} covering ${scenes.size} scenes")
        return manifest
    }

    private fun sceneJson(id: String, entry: Pair<JsonObject, MutableMap<String, JsonObject>>) =
        buildJsonObject {
            entry.first.forEach { (k, v) -> put(k, v) }
            putJsonArray("captures") {
                entry.second.toSortedMap().values.forEach { add(it) }
            }
        }

    private fun loadExistingFragments(fragmentsDir: File) {
        fragmentsDir.listFiles { f -> f.extension == "json" }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                val obj = runCatching { json.parseToJsonElement(file.readText()).jsonObject }
                    .getOrNull() ?: return@forEach
                val id = obj["id"]?.jsonPrimitive?.content ?: return@forEach
                val meta = JsonObject(obj.filterKeys { it != "captures" })
                val reRecorded = scenes.containsKey(id)
                val entry = scenes.getOrPut(id) { meta to mutableMapOf() }
                // When this run re-recorded the scene, the incoming metadata
                // declares the current states — the persisted fragment's list
                // is stale and must not be used for pruning.
                val declaredStates = (if (reRecorded) entry.first else meta)["states"]
                    ?.jsonArray
                    ?.mapNotNull { (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content }
                    ?.toSet()
                obj["captures"]?.jsonArray?.forEach { element ->
                    val capture = element as? JsonObject ?: return@forEach
                    val device = capture["device"]?.jsonPrimitive?.content ?: return@forEach
                    val state = capture["state"]?.jsonPrimitive?.content ?: "default"
                    if (reRecorded && state != "default" && declaredStates != null &&
                        state !in declaredStates
                    ) {
                        // The scene no longer declares this state — prune it.
                        return@forEach
                    }
                    val k = key(
                        capture["theme"]?.jsonPrimitive?.content ?: "light",
                        device,
                        state,
                    )
                    // This run's record wins over a persisted duplicate.
                    if (k !in entry.second) entry.second[k] = capture
                }
            }
    }

    private fun sceneMeta(scene: DocsScene): JsonObject = buildJsonObject {
        put("id", scene.id)
        put("title", scene.title)
        putJsonArray("pathTrace") { scene.pathTrace.forEach { add(it) } }
        putJsonArray("states") { scene.states.forEach { add(it) } }
        put("presentation", scene.presentation)
        put("widthDp", scene.widthDp)
        put("heightDp", scene.heightDp)
    }

    private fun writeAtomic(file: File, content: String) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(content)
        if (!tmp.renameTo(file)) {
            file.writeText(content)
            tmp.delete()
        }
    }

    private companion object {
        const val TAG = "DocsCapture"
    }
}
