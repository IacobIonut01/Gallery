/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.owncloud.di

import android.content.Context
import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.owncloud.OwnCloudProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OwnCloudModule {

    @Provides
    @Singleton
    @IntoSet
    fun provideOwnCloudProvider(
        @ApplicationContext context: Context,
        cloudMediaDao: CloudMediaDao,
        registry: ProviderRegistry
    ): MediaCapabilityProvider {
        val provider = OwnCloudProvider(context, cloudMediaDao)
        registry.register(provider)
        return provider
    }
}
