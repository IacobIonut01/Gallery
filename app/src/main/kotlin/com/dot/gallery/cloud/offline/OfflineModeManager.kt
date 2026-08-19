/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.dot.gallery.core.activeDataStore
import com.dot.gallery.feature_node.presentation.util.printDebug
import com.dot.gallery.feature_node.presentation.util.runNetworkCallbackOperation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central source of truth for cloud offline behaviour. Owns:
 * - the user's persisted offline preferences (DataStore-backed), and
 * - live network reachability (via [ConnectivityManager]).
 *
 * Exposes both reactive [StateFlow]s (for UI) and cheap `@Volatile` snapshots
 * (for the [CloudCacheInterceptor], which runs on OkHttp threads and cannot suspend).
 */
@Singleton
class OfflineModeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // === Live connectivity ===
    private val _connected = MutableStateFlow(true)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _unmetered = MutableStateFlow(true)
    val unmetered: StateFlow<Boolean> = _unmetered.asStateFlow()

    // === Persisted preferences (mirrored from DataStore) ===
    private val _forceOffline = MutableStateFlow(false)
    val forceOffline: StateFlow<Boolean> = _forceOffline.asStateFlow()

    private val _cacheOnView = MutableStateFlow(true)
    val cacheOnView: StateFlow<Boolean> = _cacheOnView.asStateFlow()

    private val _cacheWifiOnly = MutableStateFlow(false)
    val cacheWifiOnly: StateFlow<Boolean> = _cacheWifiOnly.asStateFlow()

    private val _budgetBytes = MutableStateFlow(DEFAULT_BUDGET_MB.toLong() * 1024L * 1024L)
    val budgetBytes: StateFlow<Long> = _budgetBytes.asStateFlow()

    /**
     * Whether the app should behave as offline right now: either the user forced it,
     * or there is no network. The [CloudCacheInterceptor] serves cache-only in this state.
     */
    val effectiveOffline: StateFlow<Boolean> =
        combine(_forceOffline, _connected) { forced, connected -> forced || !connected }
            .stateIn(scope, SharingStarted.Eagerly, false)

    // Cheap snapshots for the OkHttp interceptor (no suspension on the network thread).
    @Volatile var effectiveOfflineNow: Boolean = false; private set
    @Volatile var cacheOnViewNow: Boolean = true; private set
    @Volatile var cacheWifiOnlyNow: Boolean = false; private set
    @Volatile var unmeteredNow: Boolean = true; private set
    @Volatile var budgetBytesNow: Long = DEFAULT_BUDGET_MB.toLong() * 1024L * 1024L; private set

    init {
        registerConnectivity()
        observePreferences()
    }

    private fun registerConnectivity() {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        // Seed initial state. Guarded because private/work profiles may lack
        // ACCESS_NETWORK_STATE, in which case these calls throw SecurityException.
        runCatching {
            cm.activeNetwork?.let { updateFromCapabilities(cm.getNetworkCapabilities(it)) }
                ?: run { _connected.value = false }
        }.onFailure { printDebug("OfflineModeManager: connectivity seed failed: ${it.message}") }
        recomputeSnapshots()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _connected.value = true
                runCatching { updateFromCapabilities(cm.getNetworkCapabilities(network)) }
                recomputeSnapshots()
            }

            override fun onLost(network: Network) {
                _connected.value = false
                recomputeSnapshots()
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                _connected.value = true
                updateFromCapabilities(caps)
                recomputeSnapshots()
            }
        }
        if (!runNetworkCallbackOperation { cm.registerDefaultNetworkCallback(callback) }) {
            printDebug("OfflineModeManager: connectivity callback registration failed")
        }
    }

    private fun updateFromCapabilities(caps: NetworkCapabilities?) {
        _unmetered.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ?: true
    }

    private fun observePreferences() {
        scope.launch {
            context.activeDataStore.data.collect { prefs ->
                _forceOffline.value = prefs[KEY_FORCE_OFFLINE] ?: false
                _cacheOnView.value = prefs[KEY_CACHE_ON_VIEW] ?: true
                _cacheWifiOnly.value = prefs[KEY_CACHE_WIFI_ONLY] ?: false
                _budgetBytes.value = (prefs[KEY_BUDGET_MB] ?: DEFAULT_BUDGET_MB).toLong() * 1024L * 1024L
                recomputeSnapshots()
            }
        }
    }

    private fun recomputeSnapshots() {
        effectiveOfflineNow = _forceOffline.value || !_connected.value
        cacheOnViewNow = _cacheOnView.value
        cacheWifiOnlyNow = _cacheWifiOnly.value
        unmeteredNow = _unmetered.value
        budgetBytesNow = _budgetBytes.value
    }

    /** True when caching a new response is allowed under the current policy + network. */
    fun shouldWriteThrough(): Boolean =
        cacheOnViewNow && !effectiveOfflineNow && (!cacheWifiOnlyNow || unmeteredNow)

    // === Mutators (UI) ===
    suspend fun setForceOffline(enabled: Boolean) = edit { it[KEY_FORCE_OFFLINE] = enabled }
    suspend fun setCacheOnView(enabled: Boolean) = edit { it[KEY_CACHE_ON_VIEW] = enabled }
    suspend fun setCacheWifiOnly(enabled: Boolean) = edit { it[KEY_CACHE_WIFI_ONLY] = enabled }
    suspend fun setBudgetMb(mb: Int) = edit { it[KEY_BUDGET_MB] = mb.coerceIn(64, 32768) }

    val budgetMbFlow = context.activeDataStore.data.map { it[KEY_BUDGET_MB] ?: DEFAULT_BUDGET_MB }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.activeDataStore.edit(block)
    }

    companion object {
        const val DEFAULT_BUDGET_MB = 512

        private val KEY_FORCE_OFFLINE = booleanPreferencesKey("cloud_offline_force")
        private val KEY_CACHE_ON_VIEW = booleanPreferencesKey("cloud_offline_cache_on_view")
        private val KEY_CACHE_WIFI_ONLY = booleanPreferencesKey("cloud_offline_cache_wifi_only")
        private val KEY_BUDGET_MB = intPreferencesKey("cloud_offline_budget_mb")
    }
}
