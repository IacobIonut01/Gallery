@file:Suppress("KotlinConstantConditions")

package com.dot.gallery.core.logging

private const val TAG = "GalleryInfo"

// Repo rule (AGENTS.md §2): all app logging goes through these helpers — no
// direct android.util.Log.*, printStackTrace(), or println elsewhere in app
// code. The platform-log call must never break the caller or the persistent
// sink path, so it is fire-and-forget. DEBUG reaches the system log only when
// AppLogger.debugLogToSystemLog is enabled (non-release builds); every entry
// always reaches the persistent sink.
private fun logcat(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
    if (level == LogLevel.DEBUG && !AppLogger.debugLogToSystemLog) return
    platformLog(level, tag, message, throwable)
}

fun printInfo(message: Any) {
    val text = message.toString()
    logcat(LogLevel.INFO, TAG, text)
    AppLogger.sink(LogLevel.INFO, TAG, text, null, null, null)
}

fun printInfo(tag: String, message: String) {
    logcat(LogLevel.INFO, tag, message)
    AppLogger.sink(LogLevel.INFO, tag, message, null, null, null)
}

fun printDebug(message: Any) {
    printDebug(message.toString())
}

fun printDebug(message: String) {
    logcat(LogLevel.DEBUG, TAG, message)
    AppLogger.sink(LogLevel.DEBUG, TAG, message, null, null, null)
}

/** Tagged debug log — the tag prefix (e.g. `decode.heif`) sets the By-area scope. */
fun printDebug(tag: String, message: String) {
    logcat(LogLevel.DEBUG, tag, message)
    AppLogger.sink(LogLevel.DEBUG, tag, message, null, null, null)
}

fun printError(message: String) {
    logcat(LogLevel.ERROR, TAG, message)
    AppLogger.sink(LogLevel.ERROR, TAG, message, null, null, null)
}

/**
 * Structured error log. [ctx] carries debug context (counts, sizes, types);
 * [scope] names the feature domain (`cloud.immich`, `worker.upload`,
 * `media-ops`) for the By-area grouping. Values are anonymized on write.
 */
fun printError(
    tag: String,
    message: String,
    throwable: Throwable? = null,
    ctx: Map<String, String>? = null,
    scope: String? = null,
) {
    logcat(LogLevel.ERROR, tag, message, throwable)
    AppLogger.sink(LogLevel.ERROR, tag, message, throwable, ctx, scope)
}

fun printWarning(message: String) {
    logcat(LogLevel.WARN, TAG, message)
    AppLogger.sink(LogLevel.WARN, TAG, message, null, null, null)
}

fun printWarn(
    tag: String,
    message: String,
    ctx: Map<String, String>? = null,
    scope: String? = null,
) {
    logcat(LogLevel.WARN, tag, message)
    AppLogger.sink(LogLevel.WARN, tag, message, null, ctx, scope)
}

/** Records a user-facing action as a breadcrumb + EVENT log entry. */
fun logEvent(action: String, ctx: Map<String, String>? = null, scope: String? = null) {
    AppLogger.breadcrumbSink(action, ctx, scope)
}
