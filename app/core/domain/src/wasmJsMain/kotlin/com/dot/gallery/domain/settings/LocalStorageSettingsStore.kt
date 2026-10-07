/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.settings

import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** wasmJs [SettingsStore] backed by `window.localStorage` — docs-site seam. */
class LocalStorageSettingsStore : SettingsStore {

    private val flows = mutableMapOf<String, MutableStateFlow<String?>>()

    private fun rawFlow(name: String): MutableStateFlow<String?> =
        flows.getOrPut(name) { MutableStateFlow(storage.getItem(name)) }

    override fun <T> observe(key: PrefKey<T>): Flow<T> =
        rawFlow(key.name).map { raw -> raw?.let(key::decode) ?: key.default }

    override suspend fun <T> put(key: PrefKey<T>, value: T) {
        val encoded = key.encode(value)
        storage.setItem(key.name, encoded)
        rawFlow(key.name).update { encoded }
    }

    override suspend fun snapshot(): Map<String, String> =
        (0 until storage.length).mapNotNull { storage.key(it) }
            .associateWith { storage.getItem(it).orEmpty() }

    private companion object {
        val storage get() = window.localStorage
    }
}
