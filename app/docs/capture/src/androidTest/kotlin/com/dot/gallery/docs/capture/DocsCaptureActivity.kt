/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs.capture

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.dot.gallery.docs.DocsScene
import java.io.File
import kotlin.math.roundToInt
import org.json.JSONObject

/**
 * Opaque, action-bar-free host for docs-capture frames. The default
 * androidx.test `EmptyActivity` runs in a translucent invisible task, so its
 * window only ever presents a half-drawn first frame — nested Compose layers
 * never flush and captures come back blank. This activity is declared with a
 * regular opaque theme, which lets the window present normally.
 */
class DocsCaptureActivity : ComponentActivity() {

    /**
     * Direct launch support for interactive inspection and clip capture:
     *
     *   am start -n com.dot.gallery.docs.capture.test/.DocsCaptureActivity \
     *     --es scene editor-photo --es state lighting --es theme dark \
     *     --es device phone
     *
     * `device` defaults to phone. An unknown scene id or undeclared state is
     * an error and finishes the activity — there is no fallback render.
     * `--ez recordBounds true` additionally writes the measured on-screen
     * frame rect to `files/docs-frame-bounds.json` for the clip-crop tooling.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sceneId = intent.getStringExtra(EXTRA_SCENE) ?: return
        val scene = DocsSceneRegistry.findById(sceneId)
        if (scene == null) {
            Log.e(TAG, "Unknown docs scene id '$sceneId'")
            finish()
            return
        }
        val state = intent.getStringExtra(EXTRA_STATE)?.takeIf { it.isNotBlank() }
        if (state != null && state !in scene.states) {
            Log.e(TAG, "Scene '$sceneId' has no state '$state'")
            finish()
            return
        }
        val preset = runCatching {
            CapturePreset.parse(intent.getStringExtra(EXTRA_DEVICE) ?: "phone")
        }.getOrNull()?.singleOrNull()
        if (preset == null) {
            Log.e(TAG, "Unknown device preset '${intent.getStringExtra(EXTRA_DEVICE)}'")
            finish()
            return
        }
        val component = scene.presentation == "component"
        renderScene(
            widthDp = if (component) scene.widthDp else preset.widthDp,
            heightDp = if (component) scene.heightDp else preset.heightDp,
            theme = intent.getStringExtra(EXTRA_THEME),
            scene = scene,
            state = state,
            recordBounds = intent.getBooleanExtra(EXTRA_RECORD_BOUNDS, false),
        )
    }

    /**
     * Mount [scene] in the shared host pinned to an exact logical size so
     * captures are independent of the physical test display.
     */
    fun renderScene(
        widthDp: Int,
        heightDp: Int,
        theme: String?,
        scene: DocsScene,
        state: String?,
        recordBounds: Boolean = false,
    ) {
        setContent {
            CaptureContent(widthDp, heightDp, theme, scene, state, recordBounds)
        }
    }
}

@Composable
private fun CaptureContent(
    widthDp: Int,
    heightDp: Int,
    theme: String?,
    scene: DocsScene,
    state: String?,
    recordBounds: Boolean = false,
) {
    // Uniquely tags this (scene, state, theme) job so the test can prove the
    // composition under capture is the one it asked for — a stale node from a
    // previous job never carries this frame's key.
    val frameTag = "docs-frame:${scene.id}:${state ?: "default"}:${theme ?: "light"}"
    val view = LocalView.current
    val context = LocalContext.current
    Box(
        Modifier.fillMaxSize().testTag(frameTag),
        contentAlignment = Alignment.TopStart,
    ) {
        DocsSceneHost(
            theme = theme,
            scene = scene,
            state = state,
            modifier = Modifier
                .size(widthDp.dp, heightDp.dp)
                .then(
                    if (recordBounds) {
                        Modifier.onGloballyPositioned { coordinates ->
                            // Measured physical geometry for clip cropping —
                            // screen coords = window rect + the window origin.
                            val root = view.rootView
                            if (root.width <= 0 || root.height <= 0) return@onGloballyPositioned
                            val rect = coordinates.boundsInWindow()
                            val screenOrigin = IntArray(2)
                            root.getLocationOnScreen(screenOrigin)
                            val json = JSONObject()
                                .put("scene", scene.id)
                                .put("state", state ?: "default")
                                .put("theme", theme ?: "light")
                                .put("widthDp", widthDp)
                                .put("heightDp", heightDp)
                                .put("left", (rect.left + screenOrigin[0]).roundToInt())
                                .put("top", (rect.top + screenOrigin[1]).roundToInt())
                                .put("right", (rect.right + screenOrigin[0]).roundToInt())
                                .put("bottom", (rect.bottom + screenOrigin[1]).roundToInt())
                                .put("screenWidth", root.width)
                                .put("screenHeight", root.height)
                            val target = File(context.filesDir, "docs-frame-bounds.json")
                            val tmp = File(context.filesDir, "docs-frame-bounds.json.tmp")
                            tmp.writeText(json.toString())
                            tmp.renameTo(target)
                        }
                    } else {
                        Modifier
                    }
                ),
        )
    }
}

private const val TAG = "DocsCaptureActivity"
private const val EXTRA_SCENE = "scene"
private const val EXTRA_STATE = "state"
private const val EXTRA_THEME = "theme"
private const val EXTRA_DEVICE = "device"
private const val EXTRA_RECORD_BOUNDS = "recordBounds"
