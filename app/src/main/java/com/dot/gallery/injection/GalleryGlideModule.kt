package com.dot.gallery.injection

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

@GlideModule
class GalleryGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setDefaultRequestOptions(
            RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .format(DecodeFormat.PREFER_ARGB_8888)
        )
        val memoryCalculator = MemorySizeCalculator.Builder(context)
            .setMemoryCacheScreens(8f)
            .build()
        builder.setMemoryCache(LruResourceCache(memoryCalculator.memoryCacheSize.toLong()))
        builder.setIsActiveResourceRetentionAllowed(true)
        builder.setDiskCache(
            DiskLruCacheFactory(
                "${context.cacheDir}/image_cache",
                100 * 1024 * 1024
            )
        )
        val bitmapCalculator = MemorySizeCalculator.Builder(context)
            .setBitmapPoolScreens(4f)
            .build()
        builder.setBitmapPool(LruBitmapPool(bitmapCalculator.bitmapPoolSize.toLong()))
    }
}