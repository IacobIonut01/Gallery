package com.dot.gallery.core.logging

actual fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
    println("[$level] $tag: $message${throwable?.let { "\n" + it.stackTraceToString() } ?: ""}")
}
