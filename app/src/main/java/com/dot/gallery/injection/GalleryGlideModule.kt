package com.dot.gallery.injection

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class GalleryGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val calculator = MemorySizeCalculator.Builder(context)
            .setMemoryCacheScreens(2f)
            .build()
        builder.setIsActiveResourceRetentionAllowed(true)
        builder.setDiskCache(DiskLruCacheFactory("${context.cacheDir}/image_cache", 100 * 1024 * 1024))
        builder.setMemoryCache(LruResourceCache(calculator.memoryCacheSize.toLong()))
    }
}