/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.util

import android.content.Context
import android.os.Build

/**
 * Detects whether the current display can actually render HDR content, so the app can skip all HDR
 * work — the window [android.content.pm.ActivityInfo.COLOR_MODE_HDR] toggle, the gain-map probe in
 * the media viewer, and the per-image gain-map decode in the HEIC region decoder — on SDR-only
 * devices. HDR rendering (gain maps / [android.graphics.Gainmap]) requires API 34, so this always
 * reports false below that.
 */
object HdrCapabilities {

    /**
     * Desired HDR headroom cap applied via `Window.setDesiredHdrHeadroom` (API 35+) when HDR is on,
     * keeping highlights from over-brightening and washing out the surrounding SDR UI. A value of
     * `0f` means "no preference" (system default) and is used when HDR is off.
     */
    const val DESIRED_HDR_HEADROOM = 3f

    @Volatile
    private var cached: Boolean? = null

    /**
     * True when the current display advertises HDR support. The result is cached after the first
     * query (display HDR capability is effectively constant for the app's lifetime).
     */
    fun isHdrDisplay(context: Context): Boolean {
        cached?.let { return it }
        val result = compute(context)
        cached = result
        return result
    }

    private fun compute(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        return runCatching { context.resources.configuration.isScreenHdr }.getOrDefault(false)
    }
}
