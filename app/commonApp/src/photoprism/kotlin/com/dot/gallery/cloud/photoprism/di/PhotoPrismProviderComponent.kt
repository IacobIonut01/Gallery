/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.photoprism.di

import android.content.Context
import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.cloud.core.ProviderInstanceFactory
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.photoprism.PhotoPrismProvider
import com.dot.gallery.cloud.photoprism.data.api.PhotoPrismAuthInterceptor
import com.dot.gallery.core.injection.Singleton
import com.dot.gallery.data.dao.CloudMediaDao
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

/**
 * kotlin-inject contribution surface for the PhotoPrism flavour. The `nophotoprism` variant of this file declares an empty
 * interface so the component compiles when the flavour is off.
 */
interface PhotoPrismProviderComponent {

    @Singleton
    @IntoSet
    @Provides
    fun providePhotoPrismProviderFactory(
        context: Context,
        cloudMediaDao: CloudMediaDao
    ): ProviderInstanceFactory = object : ProviderInstanceFactory {
        override val providerType = ProviderType.PHOTOPRISM
        override fun create(): MediaCapabilityProvider =
            PhotoPrismProvider(context, PhotoPrismAuthInterceptor(), cloudMediaDao)
    }
}
