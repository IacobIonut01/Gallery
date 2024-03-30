package com.dot.gallery.feature_node.presentation.util

import android.app.ActivityManager
import androidx.core.content.getSystemService
import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.size.Precision
import coil3.svg.SvgDecoder
import coil3.video.VideoFrameDecoder

fun newImageLoader(
    context: PlatformContext
): ImageLoader {
    val activityManager: ActivityManager = context.getSystemService()!!
    val memoryPercent = if (activityManager.isLowRamDevice) 0.25 else 0.75
    return ImageLoader.Builder(context)
        .components {
            // SVGs
            add(SvgDecoder.Factory(false))
            // Temporarily disabled
            add(JxlDecoder.Factory())
            addPlatformComponents()
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, percent = memoryPercent)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache/coil").absoluteFile)
                .maxSizePercent(1.0)
                .build()
        }
        // Show a short crossfade when loading images asynchronously.
        .crossfade(100)
        .precision(Precision.INEXACT)
        .build()
}


private fun ComponentRegistry.Builder.addPlatformComponents() {
    // GIFs
    add(AnimatedImageDecoder.Factory())
    // Video frames
    add(VideoFrameDecoder.Factory())
}