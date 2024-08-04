package com.dot.gallery.feature_node.presentation.util

import android.app.ActivityManager
import android.graphics.Bitmap
import androidx.core.content.getSystemService
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.request.allowRgb565
import coil3.request.bitmapConfig
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.dot.gallery.core.coil.ThumbnailDecoder
import com.github.awxkee.avifcoil.decoder.HeifDecoder3

fun newImageLoader(
    context: PlatformContext
): ImageLoader {
    val activityManager: ActivityManager = context.getSystemService()!!
    val memoryPercent = if (activityManager.isLowRamDevice) 0.25 else 0.75
    return ImageLoader.Builder(context)
        .components {
            add(HeifDecoder3.Factory(context))
            // SVGs
            add(SvgDecoder.Factory(false))
            add(JxlDecoder.Factory())
            // GIFs
            add(AnimatedImageDecoder.Factory())
            // Thumbnails
            add(ThumbnailDecoder.Factory())
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
        .allowRgb565(true)
        .bitmapConfig(Bitmap.Config.HARDWARE)
        .build()
}
