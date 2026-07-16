/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder

import android.util.Log

/**
 * Lightweight, toggleable logging for the HEIC/HEIF decode + subsampling pipeline.
 *
 * Filter it in logcat with:  `adb logcat -s HeifZoom`
 * Flip [enabled] to false to silence it.
 */
internal object HeifDebug {

    @Volatile
    var enabled = true

    const val TAG = "HeifZoom"

    fun d(msg: String) {
        if (enabled) Log.d(TAG, msg)
    }

    fun w(msg: String, t: Throwable? = null) {
        if (enabled) Log.w(TAG, msg, t)
    }
}
