package com.dot.gallery.core.logging

/**
 * Fire-and-forget platform system log (logcat on Android, console elsewhere).
 * Must never throw — callers rely on logging never breaking the caller.
 */
expect fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?)
