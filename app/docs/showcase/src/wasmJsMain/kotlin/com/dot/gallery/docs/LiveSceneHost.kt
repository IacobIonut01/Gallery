/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.ui.settings.LocalSettingsStore
import com.dot.gallery.core.ui.util.AppBuildInfo
import com.dot.gallery.core.ui.util.LocalAppBuildInfo
import com.dot.gallery.domain.settings.LocalStorageSettingsStore
import com.dot.gallery.ui.theme.GalleryTheme
import com.dot.gallery.ui.theme.LocalThemePreferences
import com.dot.gallery.ui.theme.ThemePreferences
import kotlinx.browser.window
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.w3c.dom.MessageEvent

/** Host configuration driven by URL params and `docs:set` postMessages. */
private data class HostConfig(
    val scene: String? = null,
    val device: String = "phone",
    val theme: String = "auto",
    val state: String? = null,
    val seed: String? = null,
)

private fun readUrlConfig(): HostConfig {
    val params = window.location.search.removePrefix("?")
        .split('&')
        .mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx <= 0) null else pair.substring(0, idx) to pair.substring(idx + 1)
        }
        .toMap()
    return HostConfig(
        scene = params["scene"],
        device = params["device"] ?: "phone",
        theme = params["theme"] ?: "auto",
        state = params["state"],
        seed = params["seed"],
    )
}

private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun postToParent(payload: JsonObject) {
    window.parent.postMessage(payload.toString().toJsString(), "*")
}

/**
 * The embeddable docs host. Initial config comes from
 * `?scene=&device=&theme=&state=&seed=`; the embedding page can then push
 * `{"type":"docs:set", ...}` postMessages and gets `docs:ready` /
 * `docs:scenes` / `docs:error` back.
 */
@Composable
fun LiveSceneHost() {
    var config by remember { mutableStateOf(readUrlConfig()) }

    LaunchedEffect(Unit) {
        window.addEventListener("message") { event ->
            val raw = event.unsafeCast<MessageEvent>().data?.toString() ?: return@addEventListener
            val json = runCatching { Json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
            if (json == null || json.string("type") != "docs:set") return@addEventListener
            config = config.copy(
                scene = json.string("scene") ?: config.scene,
                device = json.string("device") ?: config.device,
                theme = json.string("theme") ?: config.theme,
                state = if (json.containsKey("state")) json.string("state") else config.state,
                seed = if (json.containsKey("seed")) json.string("seed") else config.seed,
            )
        }
        val scenesJson = buildString {
            append("{\"type\":\"docs:scenes\",\"scenes\":[")
            append(
                sceneRegistry.joinToString(",") { s ->
                    """{"id":"${s.id}","title":"${s.title}",""" +
                        """"pathTrace":[${s.pathTrace.joinToString(",") { "\"$it\"" }}],""" +
                        """"states":[${s.states.joinToString(",") { "\"$it\"" }}]}"""
                }
            )
            append("]}")
        }
        window.parent.postMessage(
            scenesJson.toJsString(), "*"
        )
    }

    val systemDark = isSystemInDarkTheme()
    val prefs = when (config.theme) {
        "light" -> ThemePreferences(forceTheme = true, isDarkMode = false)
        "dark" -> ThemePreferences(forceTheme = true, isDarkMode = true)
        "amoled" -> ThemePreferences(forceTheme = true, isDarkMode = true, amoledMode = true)
        else -> ThemePreferences()
    }.let { base ->
        config.seed?.let { base.copy(themeColorSeed = it.removePrefix("#")) } ?: base
    }

    CompositionLocalProvider(
        LocalThemePreferences provides prefs,
        LocalAppBuildInfo provides AppBuildInfo(versionName = "1.9.0", versionCode = 190),
        LocalSettingsStore provides remember { LocalStorageSettingsStore() },
        LocalEventHandler provides remember { DocsEventHandler() },
    ) {
        GalleryTheme(dynamicColor = false) {
            val scene = findScene(config.scene)
            if (config.scene == null) {
                ScenePicker()
            } else {
                DeviceStage(DevicePreset.parse(config.device)) {
                    scene.content(config.state)
                }
                LaunchedEffect(scene.id, config.state, config.device, config.theme) {
                    withFrameNanos { }
                    postToParent(
                        Json.parseToJsonElement(
                            """{"type":"docs:ready","scene":"${scene.id}"}"""
                        ) as JsonObject
                    )
                }
            }
        }
    }
}

@Composable
private fun ScenePicker() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Scenes", style = MaterialTheme.typography.titleLarge)
        LazyColumn {
            items(sceneRegistry) { scene ->
                ListItem(
                    headlineContent = { Text(scene.title) },
                    supportingContent = {
                        Text(scene.pathTrace.joinToString(" › ").ifEmpty { scene.id })
                    },
                    modifier = Modifier.clickable {
                        window.location.href = "?scene=${scene.id}"
                    }
                )
            }
        }
    }
}
