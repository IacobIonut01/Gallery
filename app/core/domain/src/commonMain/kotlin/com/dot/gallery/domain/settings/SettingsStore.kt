/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.settings

import kotlinx.coroutines.flow.Flow

/**
 * Portable settings backend. `DataStoreSettingsStore` (androidMain) wraps the
 * app's encrypted DataStore; `LocalStorageSettingsStore` (wasmJsMain) backs the
 * docs site with `window.localStorage`.
 */
interface SettingsStore {

    fun <T> observe(key: PrefKey<T>): Flow<T>

    suspend fun <T> put(key: PrefKey<T>, value: T)

    /**
     * Current values encoded via each key's codec, for synchronous first-frame
     * reads (`LocalPreferenceSnapshot`).
     */
    suspend fun snapshot(): Map<String, String>
}
