/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.injection

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.load.engine.executor.GlideExecutor
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@GlideModule
class GalleryGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        CoroutineScope(Dispatchers.Main).launch {
            builder.setDefaultRequestOptions(
                RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            )
            val memoryCalculator = MemorySizeCalculator.Builder(context)
                .setMemoryCacheScreens(80f)
                .build()
            builder.setMemoryCache(LruResourceCache(memoryCalculator.memoryCacheSize.toLong()))
            builder.setDiskCache(
                DiskLruCacheFactory(
                    "${context.cacheDir}/image_cache",
                    150 * 1024 * 1024
                )
            )
            builder.setIsActiveResourceRetentionAllowed(true)
            builder.setSourceExecutor(
                GlideExecutor
                    .newSourceBuilder()
                    .setThreadCount(GlideExecutor.calculateBestThreadCount())
                    .build()
            )
        }
    }
}