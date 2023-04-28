/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

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
import com.dot.gallery.core.Settings

@GlideModule
class GalleryGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val settings = Settings(context)
        val diskCacheSize = settings.diskCacheSize
        val screenCount = settings.cachedScreenCount

        builder.setDefaultRequestOptions(
            RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
        )
        val memoryCalculator = MemorySizeCalculator.Builder(context)
            .setMemoryCacheScreens(screenCount)
            .build()
        builder.setMemoryCache(LruResourceCache(memoryCalculator.memoryCacheSize.toLong()))
        builder.setDiskCache(
            DiskLruCacheFactory(
                "${context.cacheDir}/image_cache",
                diskCacheSize * 1024 * 1024
            )
        )
        val bitmapCalculator = MemorySizeCalculator.Builder(context)
            .setBitmapPoolScreens(screenCount)
            .build()
        builder.setBitmapPool(LruBitmapPool(bitmapCalculator.bitmapPoolSize.toLong()))
    }
}