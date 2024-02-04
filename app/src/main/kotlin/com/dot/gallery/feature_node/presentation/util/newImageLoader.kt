package com.dot.gallery.feature_node.presentation.util

import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import coil3.video.VideoFrameDecoder

fun newImageLoader(
    context: PlatformContext
): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            // SVGs
            add(SvgDecoder.Factory())
            // Temporarily disabled
            add(JxlDecoder.Factory())
            addPlatformComponents()
        }
        .memoryCache {
            MemoryCache.Builder()
                // Set the max size to 25% of the app's available memory.
                .maxSizePercent(context, percent = 0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache/coil").absoluteFile)
                .maxSizePercent(1.0)
                .build()
        }
        // Show a short crossfade when loading images asynchronously.
        .crossfade(true)
        .build()
}


private fun ComponentRegistry.Builder.addPlatformComponents() {
    // GIFs
    add(AnimatedImageDecoder.Factory())
    // Video frames
    add(VideoFrameDecoder.Factory())
}