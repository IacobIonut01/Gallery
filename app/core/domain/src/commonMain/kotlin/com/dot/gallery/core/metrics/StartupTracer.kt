/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.metrics

import com.dot.gallery.core.logging.AppLogger
import com.dot.gallery.core.logging.LogLevel
import kotlinx.coroutines.sync.Mutex


/**
 * Lightweight startup telemetry. All spans are logged with tag [TAG]
 * so they can be filtered with `adb logcat -s StartupPerf`.
 *
 * **Only active on debug/staging builds.** On release builds every method
 * is a no-op and [trace] simply executes the block with zero overhead.
 *
 * Platform facilities (clock, thread name, logcat) are injected once by the
 * host app so this object stays multiplatform; on wasmJs they are left as
 * no-op defaults because [ENABLED] gates every call anyway.
 *
 * Usage:
 * ```
 * val t = StartupTracer.begin("EncryptedDB.create")
 * // … work …
 * StartupTracer.end(t)
 * ```
 *
 * Or inline:
 * ```
 * val result = StartupTracer.trace("loadLibrary") { System.loadLibrary("sqlcipher") }
 * ```
 *
 * At any point call [dump] to print a summary of all recorded spans.
 */
object StartupTracer {

    private const val TAG = "StartupPerf"

    /** Non-release gate — the app sets [AppLogger.debugLogToSystemLog] from its build config. */
    val ENABLED get() = AppLogger.debugLogToSystemLog

    /** Elapsed-realtime clock (Android installs `SystemClock::elapsedRealtime`). */
    var clock: () -> Long = { 0L }

    /** Current thread name (Android installs `{ Thread.currentThread().name }`). */
    var currentThreadName: () -> String = { "" }

    /** Logcat debug line sink (Android installs `Log.d(TAG, it)`). */
    var logDebug: (String) -> Unit = {}

    /** Logcat info line sink (Android installs `Log.i(TAG, it)`). */
    var logInfo: (String) -> Unit = {}

    private val NOOP_SPAN = Span("", 0L)

    data class Span(
        val label: String,
        val startMs: Long,
        var endMs: Long = 0L,
        val threadName: String = ""
    ) {
        val durationMs get() = if (endMs > 0) endMs - startMs else -1L
    }

    private val spans = mutableListOf<Span>()
    private val lock = Mutex()
    private val appStartMs by lazy { clock() }

    fun begin(label: String): Span {
        if (!ENABLED) return NOOP_SPAN
        val span = Span(label = label, startMs = clock(), threadName = currentThreadName())
        if (lock.tryLock()) {
            try { spans.add(span) } finally { lock.unlock() }
        }
        // Rule exception (AGENTS.md §2): raw Log kept for the tag-filtered logcat stream; AppLog persists alongside.
        runCatching { logDebug("▶ $label [thread=${span.threadName}]") }
        AppLogger.sink(LogLevel.DEBUG, "app.startup", "▶ $label [thread=${span.threadName}]", null, null, null)
        return span
    }

    fun end(span: Span) {
        if (!ENABLED) return
        span.endMs = clock()
        // Rule exception (AGENTS.md §2): raw Log kept for the tag-filtered logcat stream; AppLog persists alongside.
        runCatching { logDebug("◀ ${span.label} → ${span.durationMs}ms [thread=${span.threadName}]") }
        AppLogger.sink(LogLevel.DEBUG, "app.startup", "◀ ${span.label} → ${span.durationMs}ms [thread=${span.threadName}]", null, null, null)
    }

    inline fun <T> trace(label: String, block: () -> T): T {
        if (!ENABLED) return block()
        val span = begin(label)
        return try {
            block()
        } finally {
            end(span)
        }
    }

    fun dump() {
        if (!ENABLED) return
        val totalMs = clock() - appStartMs
        if (lock.tryLock()) {
            try {
            // Rule exception (AGENTS.md §2): raw Log kept for the tag-filtered logcat stream; AppLog persists alongside.
            runCatching { logInfo("═══════════════════════════════════════════════") }
            AppLogger.sink(LogLevel.INFO, "app.startup", "═══════════════════════════════════════════════", null, null, null)
            runCatching { logInfo(" STARTUP TELEMETRY (${totalMs}ms since process start)") }
            AppLogger.sink(LogLevel.INFO, "app.startup", " STARTUP TELEMETRY (${totalMs}ms since process start)", null, null, null)
            runCatching { logInfo("═══════════════════════════════════════════════") }
            AppLogger.sink(LogLevel.INFO, "app.startup", "═══════════════════════════════════════════════", null, null, null)
            spans.sortedBy { it.startMs }.forEach { s ->
                val offset = s.startMs - appStartMs
                val dur = if (s.durationMs >= 0) "${s.durationMs}ms" else "RUNNING"
                runCatching { logInfo("  +${offset}ms  ${s.label}  $dur  [${s.threadName}]") }
                AppLogger.sink(LogLevel.INFO, "app.startup", "  +${offset}ms  ${s.label}  $dur  [${s.threadName}]", null, null, null)
            }
            runCatching { logInfo("═══════════════════════════════════════════════") }
            AppLogger.sink(LogLevel.INFO, "app.startup", "═══════════════════════════════════════════════", null, null, null)
            } finally { lock.unlock() }
        }
    }
}
