/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs.capture

import com.dot.gallery.docs.DocsScene
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Accumulates per-scene capture results and emits `docs-manifest.json`
 * fragments (DOCS_SITE_PLAN.md §7). Each recorded scene is written to
 * `manifest/<scene-id>.json` — one fragment per scene, marked
 * `fidelity: "capture"` — and the merged manifest lands at
 * `docs-manifest.json` in [outDir]. The website build merges these fragments
 * with the wasm registry export; `fidelity` is how it knows these scenes have
 * no live entry.
 */
class DocsManifestWriter(private val outDir: File) {

    private val fragments = LinkedHashMap<String, JsonObject>()
    private val captures = LinkedHashMap<String, MutableList<JsonObject>>()

    /** Record one captured frame for [scene]. [file] is relative to [outDir]. */
    fun record(scene: DocsScene, device: String, state: String?, file: String) {
        if (scene.id !in fragments) {
            fragments[scene.id] = buildJsonObject {
                put("id", scene.id)
                put("title", scene.title)
                putJsonArray("pathTrace") { scene.pathTrace.forEach { add(it) } }
                putJsonArray("states") { scene.states.forEach { add(it) } }
                put("fidelity", "capture")
            }
        }
        captures.getOrPut(scene.id) { mutableListOf() }.add(
            buildJsonObject {
                put("device", device)
                state?.let { put("state", it) }
                put("file", file)
            }
        )
    }

    /** Writes every scene fragment plus the merged manifest; returns the merged file. */
    fun write(): File {
        val fragmentsDir = File(outDir, "manifest")
        fragmentsDir.mkdirs()
        val scenesArray = buildJsonArray {
            fragments.forEach { (id, fragment) ->
                val sceneJson = buildJsonObject {
                    fragment.forEach { (k, v) -> put(k, v) }
                    putJsonArray("captures") {
                        captures[id].orEmpty().forEach { add(it) }
                    }
                }
                File(fragmentsDir, "$id.json").writeText(pretty.encodeToString(JsonObject.serializer(), sceneJson))
                add(sceneJson)
            }
        }
        val merged = File(outDir, "docs-manifest.json")
        merged.writeText(
            pretty.encodeToString(
                JsonObject.serializer(),
                buildJsonObject {
                    put("generatedAtMs", System.currentTimeMillis())
                    put("source", "docs-capture")
                    putJsonArray("scenes") { scenesArray.forEach { add(it) } }
                }
            )
        )
        return merged
    }

    private companion object {
        val pretty = Json { prettyPrint = true }
    }
}
