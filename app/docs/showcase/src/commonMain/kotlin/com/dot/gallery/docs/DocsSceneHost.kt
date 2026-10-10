/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.LocalMediaDistributor
import com.dot.gallery.core.LocalMediaSelector
import com.dot.gallery.core.LocalScrollToTop
import com.dot.gallery.core.presentation.components.LocalMediaImageRenderer
import com.dot.gallery.core.presentation.util.LocalHazeState
import com.dot.gallery.core.ui.settings.LocalSettingsStore
import com.dot.gallery.core.ui.util.AppBuildInfo
import com.dot.gallery.core.ui.util.LocalAppBuildInfo
import com.dot.gallery.core.ui.util.supportsBlur
import com.dot.gallery.domain.settings.MapSettingsStore
import com.dot.gallery.domain.settings.PrefKey
import com.dot.gallery.domain.settings.SettingsKeys
import com.dot.gallery.domain.settings.SettingsStore
import com.dot.gallery.ui.theme.GalleryTheme
import com.dot.gallery.ui.theme.LocalThemePreferences
import com.dot.gallery.ui.theme.ThemePreferences
import dev.chrisbanes.haze.blur.LocalHazeBlurStyle
import dev.chrisbanes.haze.blur.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState

/** Fixed demo clock used by registry fixtures so captures and live frames stay stable. */
const val DOCS_DEMO_EPOCH_SECONDS = 1_750_003_200L

/** Brand seed applied to docs previews so renders match the shipped palette. */
const val DOCS_BRAND_SEED = "FF005EB1"

/** Semantics tag the capture test resolves for deterministic scene-root frames. */
const val DOCS_SCENE_ROOT_TAG = "docs-scene-root"

/**
 * In-memory scene navigator for docs previews. Settings-hub rows navigate to
 * other registry scenes instead of dead-ending, and back returns to the parent.
 */
interface DocsNavigator {
    fun navigateTo(sceneId: String)
    fun navigateUp()
}

val LocalDocsNavigator = compositionLocalOf<DocsNavigator?> { null }

/**
 * Simulated window insets for scenes whose own scaffold already accepts
 * paddingValues — carries [DocsSceneHost]'s contentPadding to them so their
 * top scrim still paints from y0. Zero for every other scene.
 */
val LocalDocsContentPadding = compositionLocalOf<PaddingValues> { PaddingValues(0.dp) }

fun themeSeedFor(theme: String?): Triple<Boolean, Boolean, Boolean> = when (theme) {
    "light" -> Triple(true, false, false)
    "dark" -> Triple(true, true, false)
    "amoled" -> Triple(true, true, true)
    else -> Triple(false, false, false)
}

fun docsInitialPrefs(theme: String?, seed: String?): Map<String, String> {
    val (force, dark, amoled) = themeSeedFor(theme)
    return mapOf(
        SettingsKeys.forceTheme.name to SettingsKeys.forceTheme.encode(force),
        SettingsKeys.darkMode.name to SettingsKeys.darkMode.encode(dark),
        SettingsKeys.amoledMode.name to SettingsKeys.amoledMode.encode(amoled),
        SettingsKeys.themeColorSeed.name to
            SettingsKeys.themeColorSeed.encode(seed?.takeIf { it.isNotBlank() } ?: DOCS_BRAND_SEED),
        SettingsKeys.useSystemFont.name to SettingsKeys.useSystemFont.encode(false),
    )
}

/**
 * Shared render host for docs scenes, used identically by the wasmJs live embed
 * and the on-device capture test. Provides an isolated [MapSettingsStore]
 * (seeded from the requested preview theme, never persisted), the real
 * [GalleryTheme] driven by observed theme keys, a docs [DocsEventHandler], and
 * a [DocsNavigator] so scene-to-scene navigation inside the registry works.
 *
 * Each (scene, state, resetToken) combination gets a fresh store so state
 * changes and resets never bleed between previews or caller-driven resets.
 */
@Composable
fun DocsSceneHost(
    scene: DocsScene,
    state: String?,
    theme: String? = null,
    seed: String? = null,
    resetToken: Int = 0,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var activeId by remember(scene.id, resetToken) { mutableStateOf(scene.id) }
    val backStack = remember(scene.id, resetToken) { mutableStateListOf<String>() }
    val navigator = remember(backStack) {
        object : DocsNavigator {
            override fun navigateTo(sceneId: String) {
                if (sceneId != activeId && sceneRegistry.any { it.id == sceneId }) {
                    backStack.add(activeId)
                    activeId = sceneId
                }
            }

            override fun navigateUp() {
                if (backStack.isNotEmpty()) activeId = backStack.removeAt(backStack.lastIndex)
            }
        }
    }
    val eventHandler = remember(navigator) {
        DocsEventHandler().apply {
            navigateUpAction = { navigator.navigateUp() }
        }
    }
    val activeScene = sceneRegistry.firstOrNull { it.id == activeId } ?: scene

    // Store + gallery session live at the ROOT level: internal navigation to a
    // child scene shares them (a tweaked setting survives the trip), while the
    // site's Reset/state change remounts the whole session.
    val initialPrefs = remember(theme, seed) { docsInitialPrefs(theme, seed) }
    val store = remember(scene.id, state, theme, seed, resetToken) {
        MapSettingsStore(initialPrefs)
    }
    val session = remember(store) { DocsGallerySession(store) }

    key(activeScene.id, if (activeScene.id == scene.id) state else null, resetToken) {
        // Seed the first composition from the requested prefs — not defaults —
        // so the opening frame already renders the chosen theme.
        val forceTheme = store.observed(initialPrefs, SettingsKeys.forceTheme)
        val darkMode = store.observed(initialPrefs, SettingsKeys.darkMode)
        val amoledMode = store.observed(initialPrefs, SettingsKeys.amoledMode)
        val colorSeed = store.observed(initialPrefs, SettingsKeys.themeColorSeed)
        val systemFont = store.observed(initialPrefs, SettingsKeys.useSystemFont)

        val allowBlur = store.observed(
            initialPrefs,
            SettingsKeys.allowBlur.withDefault(supportsBlur),
        )
        val prefs = ThemePreferences(
            forceTheme = forceTheme,
            isDarkMode = darkMode,
            amoledMode = amoledMode,
            themeColorSeed = colorSeed,
            useSystemFont = systemFont,
        )

        CompositionLocalProvider(
            LocalSettingsStore provides store,
            LocalDocsNavigator provides navigator,
            LocalDocsGallerySession provides session,
            LocalEventHandler provides eventHandler,
            LocalMediaDistributor provides session.distributor,
            LocalMediaSelector provides session.selector,
            LocalMediaImageRenderer provides session.imageRenderer,
            LocalScrollToTop provides session.scrollToTop,
            LocalAppBuildInfo provides AppBuildInfo(versionName = "Preview", versionCode = 0L),
            LocalThemePreferences provides prefs,
        ) {
            GalleryTheme(dynamicColor = false) {
                // The nav bar's blur path reads these locals; the docs host
                // supplies the same isolated state + regular material MainActivity
                // provides so translucent chrome renders instead of none.
                val hazeState = rememberHazeState()
                CompositionLocalProvider(
                    LocalHazeState provides hazeState,
                    LocalHazeBlurStyle provides HazeMaterials.regular(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ).then { blurEnabled(allowBlur) },
                ) {
                    val sizing = when {
                        activeScene.presentation == "component" ->
                            Modifier.size(activeScene.widthDp.dp, activeScene.heightDp.dp)
                        else -> Modifier.fillMaxSize()
                    }
                    Box(
                        modifier
                            .then(sizing)
                            .background(MaterialTheme.colorScheme.surface)
                            .testTag(DOCS_SCENE_ROOT_TAG)
                    ) {
                        if (activeScene.handlesContentPadding) {
                            CompositionLocalProvider(LocalDocsContentPadding provides contentPadding) {
                                activeScene.content(if (activeScene.id == scene.id) state else null)
                            }
                        } else {
                            Box(Modifier.fillMaxSize().padding(contentPadding)) {
                                activeScene.content(if (activeScene.id == scene.id) state else null)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Store provider kept for call sites that compose scene content directly. */
@Composable
fun rememberDocsStore(scene: DocsScene, state: String?, theme: String?): SettingsStore =
    remember(scene.id, state, theme) { MapSettingsStore(docsInitialPrefs(theme, null)) }

/**
 * Reads [key] from [store], seeded with the host's initial prefs so the first
 * emitted frame already reflects the requested theme. The observed flow is
 * remembered per store+key so recomposition does not restart the subscriber.
 */
@Composable
private fun <T> SettingsStore.observed(initial: Map<String, String>, key: PrefKey<T>): T {
    val flow = remember(this, key) { observe(key) }
    val seeded = remember(key) { initial[key.name]?.let(key::decode) ?: key.default }
    val value by flow.collectAsState(seeded)
    return value
}
