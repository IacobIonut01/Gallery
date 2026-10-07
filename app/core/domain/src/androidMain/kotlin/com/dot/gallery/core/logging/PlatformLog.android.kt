package com.dot.gallery.core.logging

import android.util.Log

actual fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
    runCatching {
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, message, throwable)
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR, LogLevel.EVENT, LogLevel.CRASH -> Log.e(tag, message, throwable)
        }
    }
}

/**
 * Sync crash-report writer installed by the app ([AppLog.writeCrashSync]).
 * Module code calls this from uncaught-exception paths without seeing AppLog.
 */
var crashWriter: ((thread: Thread, throwable: Throwable) -> Unit)? = null
