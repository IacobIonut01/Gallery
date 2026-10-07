/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.dot.gallery.core.encryption.EncryptedDataStoreProvider
import com.dot.gallery.core.encryption.EncryptionBackendState
import com.dot.gallery.core.encryption.EncryptionStateMonitor
import com.dot.gallery.core.logging.printError
import com.dot.gallery.core.metrics.StartupTracer

const val SETTINGS_PREFERENCE_NAME = "settings"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = SETTINGS_PREFERENCE_NAME)

/**
 * Returns the active [DataStore] — the encrypted store when the device supports
 * Android Keystore-backed AES-GCM, or the default plaintext store as a fallback.
 *
 * Encryption is always enabled automatically; there is no user-facing toggle.
 */
val Context.activeDataStore: DataStore<Preferences>
    get() {
        return try {
            EncryptedDataStoreProvider.getOrCreate(this).also {
                EncryptionStateMonitor.reportSettings(EncryptionBackendState.ENCRYPTED)
            }
        } catch (e: Exception) {
            // Device doesn't support hardware-backed keystore or encryption failed.
            // Keep the app usable, but expose the fallback accurately in Security settings.
            printError("app.settings", "encryption capability probe failed", e)
            StartupTracer.trace("activeDataStore.fallbackPlaintext") { }
            EncryptionStateMonitor.reportSettings(EncryptionBackendState.PLAINTEXT_FALLBACK)
            dataStore
        }
    }
