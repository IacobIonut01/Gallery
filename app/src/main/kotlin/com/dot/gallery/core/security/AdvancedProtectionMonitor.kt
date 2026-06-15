/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.security

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.security.advancedprotection.AdvancedProtectionManager
import androidx.annotation.RequiresApi
import com.dot.gallery.feature_node.presentation.util.printDebug
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Observes Android Advanced Protection Mode (AAPM), available on Android 16+
 * via [AdvancedProtectionManager].
 *
 * When AAPM is enabled the app hardens itself automatically (see
 * [com.dot.gallery.core.Settings.Security]):
 * - Sandboxed image decoding is forced on.
 * - Metadata isolation mode is raised to at least "hybrid".
 *
 * Initialized once from [com.dot.gallery.GalleryApp.onCreate] in the main
 * process. On devices below API 36 (or if the service is unavailable) the
 * state simply stays `false` and the app behaves as before.
 */
@SuppressLint("StaticFieldLeak")
object AdvancedProtectionMonitor {

    private val _enabled = MutableStateFlow(false)

    /** Emits the current AAPM state. Always `false` on devices without the API. */
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    /** Synchronous, cached AAPM state for non-flow callers. */
    val isEnabled: Boolean
        get() = _enabled.value

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return
        initialized = true
        try {
            register(context.applicationContext)
        } catch (t: Throwable) {
            // Service unavailable or permission denied — fail silently, keep state false.
            printDebug("AdvancedProtectionMonitor init failed: ${t.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun register(context: Context) {
        val manager = context.getSystemService(AdvancedProtectionManager::class.java) ?: return
        _enabled.value = manager.isAdvancedProtectionEnabled
        printDebug("AdvancedProtectionMonitor: AAPM enabled=${_enabled.value}")
        manager.registerAdvancedProtectionCallback(
            context.mainExecutor,
            object : AdvancedProtectionManager.Callback {
                override fun onAdvancedProtectionChanged(enabled: Boolean) {
                    printDebug("AdvancedProtectionMonitor: AAPM changed=$enabled")
                    _enabled.value = enabled
                }
            }
        )
    }
}
