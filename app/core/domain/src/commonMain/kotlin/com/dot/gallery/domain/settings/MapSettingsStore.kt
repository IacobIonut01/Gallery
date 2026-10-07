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
class MapSettingsStore : SettingsStore {

    private val encoded = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun <T> observe(key: PrefKey<T>): Flow<T> =
        encoded.map { map -> map[key.name]?.let(key::decode) ?: key.default }

    override suspend fun <T> put(key: PrefKey<T>, value: T) {
        encoded.update { it + (key.name to key.encode(value)) }
    }

    override suspend fun snapshot(): Map<String, String> = encoded.value
}
