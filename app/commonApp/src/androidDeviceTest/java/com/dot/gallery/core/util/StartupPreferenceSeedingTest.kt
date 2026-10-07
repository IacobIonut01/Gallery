/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.util

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@MediumTest
@RunWith(AndroidJUnit4::class)
class StartupPreferenceSeedingTest {

    @get:Rule
    val rule = createComposeRule()

    private class FakeStore : DataStore<Preferences> {
        private val gate = CompletableDeferred<Unit>()
        private val inner = MutableStateFlow<Preferences?>(null)

        override val data: Flow<Preferences> = flow {
            gate.await()
            inner.filterNotNull().collect { emit(it) }
        }

        fun open() = gate.complete(Unit)

        fun emit(prefs: Preferences) {
            inner.value = prefs
        }

        fun current(): Preferences? = inner.value

        override suspend fun updateData(
            transform: suspend (Preferences) -> Preferences
        ): Preferences {
            val updated = transform(inner.value ?: emptyPreferences())
            inner.value = updated
            return updated
        }
    }

    @Test
    fun genericPreferenceSeedsFromSnapshotWhileTheStoreIsSuspended() {
        val key = booleanPreferencesKey("flag")
        val store = FakeStore()
        var observed by mutableStateOf<Boolean?>(null)
        rule.setContent {
            CompositionLocalProvider(
                LocalPreferenceStore provides store,
                LocalInitialPreferences provides preferencesOf(key to true)
            ) {
                val value by rememberPreference(key, false)
                SideEffect { observed = value }
            }
        }
        rule.runOnIdle { assertEquals(true, observed) }

        rule.runOnUiThread {
            store.open()
            store.emit(preferencesOf(key to false))
        }
        rule.waitUntil("live store value applied", 5_000) { observed == false }
    }

    @Test
    fun serializablePreferenceSeedsFromSnapshotWhileTheStoreIsSuspended() {
        val key = stringPreferencesKey("serial")
        val store = FakeStore()
        var observed by mutableStateOf<List<String>?>(null)
        rule.setContent {
            CompositionLocalProvider(
                LocalPreferenceStore provides store,
                LocalInitialPreferences provides preferencesOf(key to "[\"a\"]")
            ) {
                val value by rememberPreferenceSerializable(key, emptyList<String>())
                SideEffect { observed = value }
            }
        }
        rule.runOnIdle { assertEquals(listOf("a"), observed) }

        rule.runOnUiThread {
            store.open()
            store.emit(preferencesOf(key to "[\"b\",\"c\"]"))
        }
        rule.waitUntil("live store value applied", 5_000) { observed == listOf("b", "c") }
    }

    @Test
    fun newlyComposedChildrenSeedFromTheCurrentSnapshot() {
        val key = booleanPreferencesKey("flag")
        val store = FakeStore()
        var snapshot by mutableStateOf(preferencesOf(key to true))
        var showChild by mutableStateOf(false)
        var observed by mutableStateOf<Boolean?>(null)
        rule.setContent {
            CompositionLocalProvider(
                LocalPreferenceStore provides store,
                LocalInitialPreferences provides snapshot
            ) {
                if (showChild) {
                    val value by rememberPreference(key, false)
                    SideEffect { observed = value }
                }
            }
        }
        rule.waitForIdle()

        rule.runOnUiThread {
            snapshot = preferencesOf(key to false)
            showChild = true
        }
        rule.waitUntil("child composed", 5_000) { observed != null }
        rule.runOnIdle { assertEquals(false, observed) }
    }

    @Test
    fun preferenceWritesGoThroughTheProvidedStore() {
        val key = booleanPreferencesKey("flag")
        val store = FakeStore()
        var state by mutableStateOf<MutableState<Boolean>?>(null)
        rule.setContent {
            CompositionLocalProvider(
                LocalPreferenceStore provides store,
                LocalInitialPreferences provides preferencesOf(key to true)
            ) {
                val pref = rememberPreference(key, false)
                SideEffect { state = pref }
            }
        }
        rule.waitUntil("state captured", 5_000) { state != null }
        rule.runOnUiThread { state!!.value = false }
        rule.waitUntil("write reached the store", 5_000) {
            store.current()?.get(key) == false
        }
    }
}
