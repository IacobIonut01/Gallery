/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Adapts an androidx [DataStore] to the portable [SettingsStore] seam. The app
 * hands it `context.activeDataStore`, so encrypted storage stays intact.
 */
class DataStoreSettingsStore(
    private val store: DataStore<Preferences>,
) : SettingsStore {

    override fun <T> observe(key: PrefKey<T>): Flow<T> =
        if (key.type == PrefType.STRING) {
            // STRING columns hold the encoded form (identity for plain strings, JSON
            // for `PrefKey.serializable`) — decode on the way out like flat stores do.
            store.data.map { prefs ->
                prefs[stringPreferencesKey(key.name)]?.let(key::decode) ?: key.default
            }
        } else {
            store.data.map { prefs ->
                @Suppress("UNCHECKED_CAST")
                (prefs[androidxKey(key)] ?: key.default) as T
            }
        }

    override suspend fun <T> put(key: PrefKey<T>, value: T) {
        if (key.type == PrefType.STRING) {
            store.edit { it[stringPreferencesKey(key.name)] = key.encode(value) }
        } else {
            @Suppress("UNCHECKED_CAST")
            store.edit { it[androidxKey(key)] = value }
        }
    }

    override suspend fun snapshot(): Map<String, String> =
        store.data.first().toSnapshotMap()

    @Suppress("UNCHECKED_CAST")
    private fun <T> androidxKey(key: PrefKey<T>): Preferences.Key<T> = when (key.type) {
        PrefType.BOOLEAN -> booleanPreferencesKey(key.name)
        PrefType.INT -> intPreferencesKey(key.name)
        PrefType.LONG -> longPreferencesKey(key.name)
        PrefType.FLOAT -> floatPreferencesKey(key.name)
        PrefType.DOUBLE -> doublePreferencesKey(key.name)
        PrefType.STRING -> stringPreferencesKey(key.name)
        PrefType.STRING_SET -> stringSetPreferencesKey(key.name)
    } as Preferences.Key<T>
}

/** Encodes a DataStore snapshot through the string codec for `LocalPreferenceSnapshot`. */
fun Preferences.toSnapshotMap(): Map<String, String> =
    asMap().mapKeys { (k, _) -> k.name }.mapValues { (_, v) -> encodeRaw(v) }

private fun encodeRaw(value: Any?): String = when (value) {
    is Set<*> -> Json.encodeToString(SetSerializer(String.serializer()), value.filterIsInstance<String>().toSet())
    else -> value.toString()
}
