/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.dot.gallery.domain.settings.PrefKey
import com.dot.gallery.domain.settings.SettingsStore
import kotlinx.coroutines.launch

/** Platform-provided settings backend — app wraps `activeDataStore`, docs uses localStorage. */
val LocalSettingsStore = compositionLocalOf<SettingsStore?> { null }

/** Encoded snapshot (key name -> `PrefKey.encode` output) for synchronous first-frame reads. */
val LocalPreferenceSnapshot = compositionLocalOf<Map<String, String>> { emptyMap() }

/**
 * Portable repoint of `Settings.*.rememberX()`: shared callers observe
 * [PrefKey] through [LocalSettingsStore]; writes round-trip through the same
 * store the app's androidx `rememberPreference` edits.
 */
@Composable
fun <T> rememberPreference(key: PrefKey<T>): MutableState<T> {
    val store = checkNotNull(LocalSettingsStore.current) {
        "rememberPreference requires LocalSettingsStore — provide a SettingsStore at the composition root"
    }
    val scope = rememberCoroutineScope()
    val snapshot = LocalPreferenceSnapshot.current
    val initial = snapshot[key.name]?.let(key::decode) ?: key.default
    val state by store.observe(key).collectAsState(initial)
    return remember(state) {
        object : MutableState<T> {
            override var value: T
                get() = state
                set(value) {
                    scope.launch { store.put(key, value) }
                }

            override fun component1() = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}
