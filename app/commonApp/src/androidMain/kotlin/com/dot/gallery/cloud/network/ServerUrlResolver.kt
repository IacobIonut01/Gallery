/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.network

import android.content.Context
import com.dot.gallery.cloud.core.CloudServerConfig
import com.dot.gallery.core.presentation.util.currentWifiSsid
import com.dot.gallery.core.presentation.util.isOnLocalNetwork
import javax.inject.Inject
import com.dot.gallery.core.injection.Singleton


/**
 * Resolves the effective server URL for a [CloudServerConfig] based on the current network.
 *
 * When [CloudServerConfig.autoUrlSwitch] is enabled and a [CloudServerConfig.localServerUrl]
 * is configured, the local URL is used while the device is on the configured local network:
 *  - a [CloudServerConfig.localWifiSsid] MUST be set — switching only ever happens on that
 *    named network. A blank SSID means no local network is configured, so switching is
 *    effectively disabled and the external URL is always used.
 *  - if the SSID is unreadable (no location permission on Android 10+), any Wi-Fi/Ethernet
 *    (local) connection is treated as the configured network so the feature still works.
 *
 * Otherwise the externally-reachable [CloudServerConfig.serverUrl] is used unchanged.
 */
@Singleton
class ServerUrlResolver @Inject constructor(
    private val context: Context
) {

    /** Returns a copy of [config] whose [CloudServerConfig.serverUrl] is the effective URL. */
    fun resolve(config: CloudServerConfig): CloudServerConfig {
        val effective = effectiveUrl(config)
        return if (effective == config.serverUrl) config else config.copy(serverUrl = effective)
    }

    /** Computes the effective base URL without copying the config. */
    fun effectiveUrl(config: CloudServerConfig): String {
        if (!config.autoUrlSwitch || config.localServerUrl.isBlank()) return config.serverUrl
        if (!isOnConfiguredLocalNetwork(config)) return config.serverUrl
        return config.localServerUrl.trimEnd('/')
    }

    /** Whether the device is currently on the local network the [config] targets. */
    fun isOnConfiguredLocalNetwork(config: CloudServerConfig): Boolean {
        val targetSsid = config.localWifiSsid.trim()
        // No network configured -> switching is disabled, always use the external URL.
        if (targetSsid.isBlank()) return false
        if (!context.isOnLocalNetwork()) return false
        val currentSsid = context.currentWifiSsid()
        // The SSID is unreadable on Android 10+ without ACCESS_FINE_LOCATION (+ enabled
        // location services); the system reports "<unknown ssid>" -> currentWifiSsid() == null.
        // In that case degrade to "any local Wi-Fi/Ethernet counts as the configured network"
        // so URL switching still works instead of silently never matching.
        if (currentSsid == null) return true
        return currentSsid.equals(targetSsid, ignoreCase = true)
    }
}
