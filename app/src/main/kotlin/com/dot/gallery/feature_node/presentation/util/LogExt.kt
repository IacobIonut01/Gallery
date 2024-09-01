@file:Suppress("KotlinConstantConditions")

package com.dot.gallery.feature_node.presentation.util

import android.util.Log
import com.dot.gallery.BuildConfig

fun printDebug(message: Any) {
    printDebug(message.toString())
}

fun printDebug(message: String) {
    if (BuildConfig.BUILD_TYPE != "release") {
        Log.d("GalleryInfo", message)
    }
}

fun printError(message: String) {
    Log.e("GalleryInfo", message)
}

fun printWarning(message: String) {
    if (BuildConfig.BUILD_TYPE != "release") {
        Log.w("GalleryInfo", message)
    }
}