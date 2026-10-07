/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs.capture

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.docs.DocsScene
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Capture tier runner (DOCS_SITE_PLAN.md §7). Iterates the shared docs scene
 * registry, renders each scene/state under the [CapturePreset] device overrides
 * and writes WebP frames plus a `docs-manifest.json` fragment set into the test
 * app's `files/docs-out` — pulled by `testing/test-lab.sh docs-capture`.
 *
 * Instrumentation args (all optional, `-e name value`):
 *  - `scene`  comma-separated scene ids (default: every registered scene)
 *  - `device` comma-separated presets: phone, tablet, foldable (default: all)
 *  - `state`  single named state (default: every declared state)
 *  - `theme`  light | dark | amoled | system (default: light — deterministic)
 *
 * Note: `ForcedSize` simulates preset viewports inside the host window; on
 * displays smaller than a preset the frame is clipped by the window. Run on a
 * large-display emulator (or accept clipping) until the CI job owns a device
 * pool per preset.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class DocsCaptureTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<DocsCaptureActivity>()

    @Test
    fun captureScenes() {
        val args = InstrumentationRegistry.getArguments()
        val scenes = DocsSceneRegistry.select(args.getString("scene"))
        val presets = CapturePreset.parse(args.getString("device"))
        val stateFilter = args.getString("state")
        val theme = args.getString("theme") ?: "light"

        val base = composeRule.activity.getExternalFilesDir(null) ?: composeRule.activity.filesDir
        val outDir = File(base, "docs-out").apply { mkdirs() }
        val writer = DocsManifestWriter(outDir)

        val jobs = mutableListOf<Triple<DocsScene, CapturePreset, String?>>()
        for (scene in scenes) {
            for (preset in presets) {
                for (state in DocsSceneRegistry.statesOf(scene, stateFilter)) {
                    jobs += Triple(scene, preset, state)
                }
            }
        }
        Log.i(TAG, "docs-capture: ${jobs.size} capture(s) over ${scenes.size} scene(s) x ${presets.size} preset(s)")

        // setContent is single-shot per activity — host a mutable frame slot and
        // recompose per job instead of calling setContent in the loop.
        val frame = mutableStateOf<Triple<DocsScene, CapturePreset, String?>?>(null)
        composeRule.setContent {
            frame.value?.let { (scene, preset, state) ->
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(
                        DpSize(preset.widthDp.dp, preset.heightDp.dp)
                    )
                ) {
                    DocsSceneHost(theme = theme, scene = scene, state = state)
                }
            }
        }

        jobs.forEachIndexed { i, (scene, preset, state) ->
            Log.i(
                TAG,
                "docs-capture [${i + 1}/${jobs.size}] scene=${scene.id} device=${preset.id} state=${state ?: "default"}"
            )
            val relative = captureFrame(scene, preset, state, frame, outDir)
            writer.record(scene, preset.id, state, relative)
        }

        val manifest = writer.write()
        Log.i(TAG, "docs-capture: wrote ${jobs.size} capture(s); manifest at ${manifest.absolutePath}")
    }

    private fun captureFrame(
        scene: DocsScene,
        preset: CapturePreset,
        state: String?,
        frame: androidx.compose.runtime.MutableState<Triple<DocsScene, CapturePreset, String?>?>,
        outDir: File,
    ): String {
        composeRule.runOnUiThread { frame.value = Triple(scene, preset, state) }
        composeRule.waitForIdle()

        val root = composeRule.onRoot().fetchSemanticsNode()
        Log.i(
            TAG,
            "docs-capture frame root bounds=${root.boundsInRoot} children=${root.children.size}"
        )

        // Window-level captures (PixelCopy / view.draw) come back blank in this
        // harness. captureToImage records the node's own draw commands — scope
        // it to the first content child so the decorview title bar is excluded.
        val bitmap = captureSceneBitmap()
        val relative = buildString {
            append(scene.id).append('/').append(preset.id)
            state?.let { append('-').append(it) }
            append(".webp")
        }
        val file = File(outDir, relative).apply { parentFile?.mkdirs() }
        FileOutputStream(file).use { stream ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, WEBP_QUALITY, stream)
            } else {
                @Suppress("DEPRECATION")
                bitmap.compress(Bitmap.CompressFormat.WEBP, WEBP_QUALITY, stream)
            }
        }
        return relative
    }

    private fun captureSceneBitmap(): Bitmap {
        val root = composeRule.onRoot().fetchSemanticsNode()
        val target = root.children.firstOrNull() ?: root
        val matcher = SemanticsMatcher(
            "docs-scene-root(${target.id})"
        ) { it.id == target.id }
        val node = composeRule.onAllNodes(matcher, useUnmergedTree = true).onFirst()
        return node.captureToImage().asAndroidBitmap()
    }

    private companion object {
        const val TAG = "DocsCapture"
        const val WEBP_QUALITY = 85
    }
}
