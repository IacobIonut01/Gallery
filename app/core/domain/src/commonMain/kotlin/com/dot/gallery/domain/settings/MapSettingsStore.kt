/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [SettingsStore] for docs captures and instrumented tests — the
 * in-memory analogue of `LocalStorageSettingsStore`/`DataStoreSettingsStore`.
 * Defaults come from each [PrefKey]; `put` round-trips through the key's string
 * codec so snapshot and observe stay consistent with the flat-store
 * implementations.
 */
class MapSettingsStore(initial: Map<String, String> = emptyMap()) : SettingsStore {

    private val encoded = MutableStateFlow<Map<String, String>>(initial)

    override fun <T> observe(key: PrefKey<T>): Flow<T> =
        encoded.map { map -> map[key.name]?.let(key::decode) ?: key.default }

    override suspend fun <T> put(key: PrefKey<T>, value: T) {
        encoded.update { it + (key.name to key.encode(value)) }
    }

    /**
     * Synchronously overlays [entries] (already-encoded values). Docs scene
     * fixtures use this inside `remember {}` during composition so the first
     * emitted frame already carries the seeded preferences — a suspend `put`
     * would only take effect on the following frame.
     */
    fun seed(entries: Map<String, String>) {
        encoded.update { it + entries }
    }

    /** Convenience [seed] for typed keys — encodes through each [PrefKey]. */
    fun seed(vararg pairs: Pair<PrefKey<*>, *>) {
        val entries = pairs.associate { (key, value) ->
            @Suppress("UNCHECKED_CAST")
            key.name to (key as PrefKey<Any?>).encode(value)
        }
        seed(entries)
    }

    override suspend fun snapshot(): Map<String, String> = encoded.value
}
