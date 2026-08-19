/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.encryption

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.dot.gallery.core.dataStore
import com.dot.gallery.core.metrics.StartupTracer
import com.dot.gallery.feature_node.presentation.util.printDebug
import com.dot.gallery.feature_node.presentation.util.printWarning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

enum class EncryptionBackendState {
    UNKNOWN,
    ENCRYPTED,
    PLAINTEXT_FALLBACK,
}

data class EncryptionRuntimeState(
    val settings: EncryptionBackendState = EncryptionBackendState.UNKNOWN,
    val database: EncryptionBackendState = EncryptionBackendState.UNKNOWN,
) {
    val hasPlaintextFallback: Boolean
        get() = settings == EncryptionBackendState.PLAINTEXT_FALLBACK ||
            database == EncryptionBackendState.PLAINTEXT_FALLBACK

    val fullyEncrypted: Boolean
        get() = settings == EncryptionBackendState.ENCRYPTED &&
            database == EncryptionBackendState.ENCRYPTED
}

/** Runtime source of truth for what the app actually opened, not what it intended to open. */
object EncryptionStateMonitor {
    private val _state = MutableStateFlow(EncryptionRuntimeState())
    val state: StateFlow<EncryptionRuntimeState> = _state.asStateFlow()

    fun reportSettings(state: EncryptionBackendState) {
        _state.update { it.copy(settings = state) }
    }

    fun reportDatabase(state: EncryptionBackendState) {
        _state.update { it.copy(database = state) }
    }
}

/**
 * Manages the encrypted DataStore lifecycle.
 *
 * On first access, automatically migrates any existing plaintext preferences
 * into the encrypted store. Subsequent accesses return the encrypted store
 * directly.
 */
object EncryptedDataStoreProvider {

    private const val ENCRYPTED_STORE_NAME = "encrypted_settings.pb"
    private const val PREFS_NAME = "encrypted_datastore_flags"
    private const val FLAG_MIGRATED = "prefs_migrated"

    @Volatile
    private var instance: DataStore<Preferences>? = null

    fun getOrCreate(context: Context): DataStore<Preferences> {
        return instance ?: synchronized(this) {
            instance ?: run {
                val span = StartupTracer.begin("EncryptedDataStore.getOrCreate")
                val store = StartupTracer.trace("EncryptedDataStore.create") {
                    createEncryptedDataStore(context)
                }
                StartupTracer.trace("EncryptedDataStore.autoMigrate") {
                    autoMigrateIfNeeded(context, store)
                }
                instance = store
                StartupTracer.end(span)
                store
            }
        }
    }

    private fun createEncryptedDataStore(context: Context): DataStore<Preferences> {
        val serializer = EncryptedPreferencesSerializer(context)
        val file = File(context.filesDir, "datastore/$ENCRYPTED_STORE_NAME")
        file.parentFile?.mkdirs()
        return DataStoreFactory.create(
            serializer = serializer,
            produceFile = { file }
        )
    }

    /**
     * On first launch with encryption, copies all existing plaintext preferences
     * into the encrypted store and clears the plaintext store. Uses a
     * SharedPreferences flag to ensure this runs only once.
     */
    private fun autoMigrateIfNeeded(context: Context, encryptedStore: DataStore<Preferences>) {
        val flags = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (flags.getBoolean(FLAG_MIGRATED, false)) return

        try {
            runBlocking(Dispatchers.IO) {
                val plaintextStore = context.dataStore
                val currentPrefs = plaintextStore.data.first()

                if (currentPrefs.asMap().isNotEmpty()) {
                    encryptedStore.edit { encPrefs ->
                        currentPrefs.asMap().forEach { (key, value) ->
                            @Suppress("UNCHECKED_CAST")
                            encPrefs[key as Preferences.Key<Any>] = value
                        }
                    }
                    plaintextStore.edit { it.clear() }
                    printDebug("EncryptedDataStoreProvider: migrated ${currentPrefs.asMap().size} preferences to encrypted store")
                }
            }
            flags.edit().putBoolean(FLAG_MIGRATED, true).apply()
        } catch (e: Exception) {
            printWarning("EncryptedDataStoreProvider: auto-migration failed: ${e.message}")
        }
    }
}
