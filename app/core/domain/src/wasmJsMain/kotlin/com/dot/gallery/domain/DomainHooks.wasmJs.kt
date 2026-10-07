package com.dot.gallery.domain


actual fun platformLogError(tag: String, message: String, throwable: Throwable?) {
    println("ERROR [$tag] $message ${throwable?.stackTraceToString().orEmpty()}")
}
