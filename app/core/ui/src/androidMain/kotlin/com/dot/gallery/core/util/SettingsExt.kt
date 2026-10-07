/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.dot.gallery.core.activeDataStore


val LocalInitialPreferences = staticCompositionLocalOf<Preferences?> { null }

val LocalPreferenceStore = staticCompositionLocalOf<DataStore<Preferences>?> { null }

@Composable
fun <T> rememberPreference(
    key: Preferences.Key<T>,
    defaultValue: T,
): MutableState<T> {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val store = LocalPreferenceStore.current ?: context.activeDataStore
    val initial = LocalInitialPreferences.current?.get(key) ?: defaultValue
    val state by remember(store, key, defaultValue) {
        store.data
            .map { it[key] ?: defaultValue }
    }.collectAsStateWithLifecycle(initialValue = initial)

    return remember(state) {
        object : MutableState<T> {
            override var value: T
                get() = state
                set(value) {
                    coroutineScope.launch {
                        store.edit {
                            it[key] = value
                        }
                    }
                }

            override fun component1() = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@Composable
inline fun <reified T> rememberPreferenceSerializable(
    keyString: Preferences.Key<String>,
    defaultValue: T,
): MutableState<T> {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val store = LocalPreferenceStore.current ?: context.activeDataStore
    val initial = LocalInitialPreferences.current?.get(keyString)
        ?: Json.encodeToString(defaultValue)
    val state by remember(store, keyString, defaultValue) {
        store.data
            .map { it[keyString] ?: Json.encodeToString(defaultValue) }
    }.collectAsStateWithLifecycle(initialValue = initial)

    return remember(state) {
        object : MutableState<T> {
            override var value: T
                get() = Json.decodeFromString(state)
                set(value) {
                    coroutineScope.launch {
                        store.edit {
                            it[keyString] = Json.encodeToString(value)
                        }
                    }
                }

            override fun component1() = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}