/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dot.gallery.ui.theme.GalleryTheme
import kotlin.js.JsString
import kotlin.js.toJsString
import kotlinx.browser.window
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.w3c.dom.MessageEvent
import org.w3c.dom.Window
import org.w3c.dom.events.Event

private data class LiveConfig(
    val scene: String?,
    val state: String?,
    val theme: String?,
    val seed: String?,
    val statusBar: Boolean = false,
    val statusBarHeight: Int = 32,
)

private fun readUrlConfig(): LiveConfig {
    val params = window.location.search.removePrefix("?").split("&")
        .filter { it.isNotBlank() }
        .mapNotNull {
            val eq = it.indexOf('=')
            if (eq <= 0) null else it.substring(0, eq) to it.substring(eq + 1)
        }.toMap()
    fun decode(v: String) = jsDecodeURIComponent(v)
    return LiveConfig(
        scene = params["scene"]?.let(::decode),
        state = params["state"]?.let(::decode),
        theme = params["theme"]?.let(::decode),
        seed = params["seed"]?.let(::decode),
        statusBar = params["statusBar"] == "true",
        statusBarHeight = params["statusBarHeight"]?.toIntOrNull()?.coerceIn(0, 128) ?: 32,
    )
}

private fun jsDecode(v: JsString): JsString = js("decodeURIComponent(v)")

private fun jsDecodeURIComponent(v: String): String = jsDecode(v.toJsString()).toString()

private fun isEmbedded(): Boolean = js("window.parent !== window.self")

private fun sourceEquals(event: MessageEvent, target: Window): Boolean =
    js("event.source === target")

private fun postToParent(payload: String) {
    window.parent.postMessage(payload.toJsString(), "*")
}

private fun sceneCatalogPayload(): String {
    val payload = buildJsonObject {
        put("type", "docs:scenes")
        putJsonArray("scenes") {
            sceneRegistry.forEach { scene ->
                add(buildJsonObject {
                    put("id", scene.id)
                    put("title", scene.title)
                    putJsonArray("pathTrace") { scene.pathTrace.forEach { add(it) } }
                    putJsonArray("states") { scene.states.forEach { add(it) } }
                    put("presentation", scene.presentation)
                    put("widthDp", scene.widthDp)
                    put("heightDp", scene.heightDp)
                })
            }
        }
    }
    return payload.toString()
}

private fun docsMessage(type: String, vararg fields: Pair<String, String?>): String {
    val payload = buildJsonObject {
        put("type", type)
        fields.forEach { (k, v) -> if (v != null) put(k, v) }
    }
    return payload.toString()
}

private fun docsReadyPayload(scene: DocsScene, state: String?, theme: String?): String {
    val payload = buildJsonObject {
        put("type", "docs:ready")
        put("scene", scene.id)
        if (state != null) put("state", state)
        if (theme != null) put("theme", theme)
        putJsonObject("size") {
            put("width", scene.widthDp)
            put("height", scene.heightDp)
        }
    }
    return payload.toString()
}

/**
 * Browser entrypoint for the docs live bundle.
 *
 * Protocol with the embedding site (postMessage):
 *  - host → parent `docs:scenes` once at startup with the full registry catalog.
 *  - parent → host `docs:set` `{scene?, state?, theme?, seed?, reset?}`; accepted
 *    only from the embedding parent frame (same origin) or from self when the
 *    bundle is opened standalone.
 *  - host → parent `docs:ready` after the first rendered frame of each scene
 *    configuration, and `docs:error` for unknown scene ids.
 */
@Composable
fun LiveSceneHost() {
    var config by remember { mutableStateOf(readUrlConfig()) }
    var resetToken by remember { mutableIntStateOf(0) }
    val embedded = remember { isEmbedded() }

    DisposableEffect(embedded) {
        val listener: (Event) -> Unit = listener@{ event ->
            val message = event.unsafeCast<MessageEvent>()
            val sourceOk = if (embedded) {
                sourceEquals(message, window.parent)
            } else {
                sourceEquals(message, window.self) || message.source == null
            }
            val originOk = message.origin == window.location.origin
            if (sourceOk && originOk) {
                val raw = message.data?.toString() ?: return@listener
                val json = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull()
                if (json != null && json["type"]?.jsonPrimitive?.content == "docs:set") {
                    fun str(key: String) = json[key]?.jsonPrimitive?.content
                    config = config.copy(
                        scene = str("scene") ?: config.scene,
                        state = str("state") ?: config.state,
                        theme = str("theme") ?: config.theme,
                        seed = str("seed") ?: config.seed,
                    )
                    if (json["reset"]?.jsonPrimitive?.content == "true") {
                        resetToken += 1
                    }
                }
            }
        }
        window.addEventListener("message", listener)
        onDispose { window.removeEventListener("message", listener) }
    }

    LaunchedEffect(Unit) {
        postToParent(sceneCatalogPayload())
    }

    val requested = config.scene
    val scene = findSceneOrNull(requested)

    LaunchedEffect(scene?.id, config.state, config.theme, resetToken) {
        if (requested == null) return@LaunchedEffect
        if (scene == null) {
            postToParent(docsMessage("docs:error", "scene" to requested, "reason" to "unknown-scene"))
        } else {
            // Ready only once a real frame of the scene has committed.
            withFrameNanos { }
            postToParent(docsReadyPayload(scene, config.state, config.theme))
        }
    }

    when {
        scene != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                DocsSceneHost(
                    scene = scene,
                    state = config.state,
                    theme = config.theme,
                    seed = config.seed,
                    resetToken = resetToken,
                    modifier = if (scene.presentation == "component") Modifier else Modifier.fillMaxSize(),
                    contentPadding = if (config.statusBar && scene.presentation != "component") {
                        PaddingValues(top = config.statusBarHeight.dp)
                    } else {
                        PaddingValues(0.dp)
                    },
                )
            }
        }

        requested != null -> {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
        }

        else -> ScenePicker()
    }
}

@Composable
private fun ScenePicker() {
    val scheme = MaterialTheme.colorScheme
    GalleryTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Text(
                "ReFra docs scenes",
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                modifier = Modifier.padding(16.dp),
            )
            LazyColumn {
                items(sceneRegistry) { scene ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(scene.title, color = scheme.onSurface)
                        Text(
                            scene.pathTrace.joinToString(" › "),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
