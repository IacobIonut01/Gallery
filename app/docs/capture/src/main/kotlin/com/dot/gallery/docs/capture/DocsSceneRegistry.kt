/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs.capture

import com.dot.gallery.docs.DocsScene
import com.dot.gallery.docs.sceneRegistry

/**
 * Capture-tier view over the shared scene registry (DOCS_SITE_PLAN.md §6). The
 * single source of truth is `sceneRegistry` in :docs-showcase commonMain — the
 * same list the wasmJs `LiveSceneHost` enumerates — so capture and live tiers
 * can never drift apart. This object only adds selection/filtering the
 * instrumented runner needs.
 */
object DocsSceneRegistry {

    /** Every registered docs scene, in registry order. */
    val scenes: List<DocsScene>
        get() = sceneRegistry

    fun findById(id: String): DocsScene? = scenes.firstOrNull { it.id == id }

    /**
     * Scenes to capture for a `--scene`/`-e scene` filter: null or empty selects
     * the whole registry; a comma-separated id list selects that subset.
     * Unknown ids fail fast so a typo never silently captures nothing.
     */
    fun select(filter: String?): List<DocsScene> {
        if (filter.isNullOrBlank()) return scenes
        val wanted = filter.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val unknown = wanted.filter { id -> scenes.none { it.id == id } }
        require(unknown.isEmpty()) {
            "Unknown docs scene id(s): ${unknown.joinToString()}. " +
                "Registered: ${scenes.joinToString { it.id }}"
        }
        return scenes.filter { it.id in wanted }
    }

    /**
     * States to render per scene. A scene with no declared states captures once
     * (null state); [filter] narrows to a single named state and fails fast on
     * a name the scene does not declare. States in [DocsScene.recordedStates]
     * are clip material, not stills: an unfiltered run skips them and an
     * explicit filter naming one fails — it must be produced by the recording
     * workflow, never overwritten by a still.
     */
    fun statesOf(scene: DocsScene, filter: String?): List<String?> {
        val declared = scene.states.ifEmpty { listOf(null) }
        if (filter.isNullOrBlank()) return declared.filter { it !in scene.recordedStates }
        if (filter in scene.recordedStates) {
            error(
                "State '$filter' of scene '${scene.id}' is a recorded state — capture it with " +
                    "the recording workflow (DocsCaptureActivity + --ez recordBounds true + " +
                    "screenrecord), not a still capture."
            )
        }
        require(filter in scene.states) {
            "Scene '${scene.id}' has no state '$filter'. Declared: ${scene.states.joinToString()}"
        }
        return listOf(filter)
    }
}
