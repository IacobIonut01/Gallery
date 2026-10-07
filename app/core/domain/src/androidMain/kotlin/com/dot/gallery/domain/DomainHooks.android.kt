package com.dot.gallery.domain

import android.util.Log

actual fun platformLogError(tag: String, message: String, throwable: Throwable?) {
    Log.e(tag, message, throwable)
}
