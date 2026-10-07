/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.immich.di

import android.content.Context
import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.cloud.core.ProviderInstanceFactory
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.data.dao.CloudMediaDao
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import com.dot.gallery.core.injection.Singleton
import com.dot.gallery.cloud.immich.ImmichProvider
import com.dot.gallery.cloud.immich.data.api.ImmichAuthInterceptor

/**
 * kotlin-inject contribution surface for the Immich flavour. The `noimmich` variant of this file declares an empty
 * interface so the component compiles when the flavour is off.
 */
interface ImmichProviderComponent {

    @Singleton
    @IntoSet
    @Provides
    fun provideImmichProviderFactory(
        context: Context,
        cloudMediaDao: CloudMediaDao
    ): ProviderInstanceFactory = object : ProviderInstanceFactory {
        override val providerType = ProviderType.IMMICH
        override fun create(): MediaCapabilityProvider =
            ImmichProvider(context, ImmichAuthInterceptor(), cloudMediaDao)
    }
}

