package com.dot.gallery.core.logging

/**
 * Indirection between the module-level `print*`/`logEvent` helpers and the
 * app-side persistent [AppLog] implementation. The app installs the sinks once
 * at startup (see `GalleryApp.onCreate`); until then, calls are no-ops.
 */
object AppLogger {

    /** Persistent log sink — the app installs `AppLog::log`. */
    var sink: (
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        ctx: Map<String, String>?,
        scope: String?,
    ) -> Unit = { _, _, _, _, _, _ -> }

    /** Breadcrumb sink — the app installs `AppLog::breadcrumb`. */
    var breadcrumbSink: (label: String, ctx: Map<String, String>?, scope: String?) -> Unit =
        { _, _, _ -> }

    /**
     * Whether DEBUG-level `print*` calls also reach the platform system log
     * (logcat/console). The app sets this from its build config (off for
     * release builds); non-debug levels always reach the system log.
     */
    var debugLogToSystemLog: Boolean = false
}
