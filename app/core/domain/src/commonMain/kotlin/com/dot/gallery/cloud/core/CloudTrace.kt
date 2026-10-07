/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import com.dot.gallery.core.AppIdentity
import com.dot.gallery.core.logging.LogLevel
import com.dot.gallery.core.logging.platformLog
import kotlin.math.roundToInt
import kotlin.time.TimeSource


/**
 * Lightweight tracing for the cloud media pipeline (all providers: SMB/NFS, WebDAV, Immich, …).
 *
 * Logs to the `CloudTrace` tag and is a no-op on release builds, so the instrumentation can stay
 * permanently in place. Use [time]/[timeNullable] to record how long each stage takes — the slow
 * SMB loads are easiest to diagnose by seeing which stage (network read vs decode) dominates.
 *
 * Filter logs with: `adb logcat -s CloudTrace`
 */
object CloudTrace {
    const val TAG = "CloudTrace"

    /**
     * Host-provided persistent sink (the app's [AppLog] file logger) installed once at startup so
     * module code can emit persistent entries without seeing the app-side logger.
     */
    var persistentSink: (LogLevel, String, String, Throwable?) -> Unit = { _, _, _, _ -> }

    /**
     * Host-provided gate mirroring the user's "Verbose logging" cloud Advanced setting so release
     * builds can be diagnosed on demand.
     */
    var verboseLogging: () -> Boolean = { false }

    /** Always on for non-release builds (debug + staging). */
    private val buildEnabled: Boolean get() = !AppIdentity.isReleaseBuild

    /**
     * Enabled on non-release builds, or whenever the user turns on "Verbose logging" in the
     * cloud Advanced settings (so release builds can be diagnosed on demand).
     */
    val enabled: Boolean get() = buildEnabled || verboseLogging()

    private val appLogTag: String = if ('.' in TAG) TAG else "cloud.trace"

    fun d(message: String) {
        if (enabled) {
            // platformLog -> android.util.Log on Android, keeping the `adb logcat -s CloudTrace` stream.
            platformLog(LogLevel.DEBUG, TAG, message, null)
            persistentSink(LogLevel.DEBUG, appLogTag, message, null)
        }
    }

    fun w(message: String, t: Throwable? = null) {
        if (enabled) {
            platformLog(LogLevel.WARN, TAG, message, t)
            persistentSink(LogLevel.WARN, appLogTag, message, t)
        }
    }

    /** Human-readable byte size, e.g. "14.0 MB" — one decimal via roundToInt (no String.format on wasmJs). */
    fun bytes(n: Long): String = when {
        n < 0 -> "?"
        n < 1024 -> "$n B"
        n < 1024 * 1024 -> "${(n / 102.4).roundToInt() / 10.0} KB"
        else -> "${(n / (1024.0 * 102.4)).roundToInt() / 10.0} MB"
    }

    /** Times [block], logging "<label> took <ms>ms" (annotating failures). */
    inline fun <T> time(label: String, block: () -> T): T {
        if (!enabled) return block()
        val mark = TimeSource.Monotonic.markNow()
        var failure: Throwable? = null
        try {
            return block()
        } catch (t: Throwable) {
            failure = t
            throw t
        } finally {
            val ms = mark.elapsedNow().inWholeMilliseconds
            if (failure == null) d("$label took ${ms}ms")
            else w("$label FAILED after ${ms}ms: $failure")
        }
    }
}
