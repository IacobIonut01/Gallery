/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat


/**
 * Remembers a callback that requests [Manifest.permission.ACCESS_FINE_LOCATION] at runtime if it
 * is not already granted. This permission is needed for the cloud local/external URL switching to
 * read the current Wi-Fi SSID and match it against a configured network name (Android 10+ hides
 * the SSID from apps without it). If the user denies it, [ServerUrlResolver] degrades gracefully
 * to treating any Wi-Fi/Ethernet connection as "local".
 *
 * The permission is stripped from the offline variant's manifest, so on offline builds the request
 * simply resolves as denied and the caller's SSID matching falls back to the graceful default.
 */
@Composable
fun rememberWifiSsidPermissionRequester(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Result ignored: ServerUrlResolver degrades gracefully when denied. */ }
    return remember(context) {
        {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}
