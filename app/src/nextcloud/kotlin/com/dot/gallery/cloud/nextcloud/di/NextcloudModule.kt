/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.nextcloud.di

import android.content.Context
import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.cloud.core.ProviderInstanceFactory
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.auth.CloudInteractiveAuthHandler
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.nextcloud.NextcloudDialect
import com.dot.gallery.cloud.nextcloud.auth.NextcloudInteractiveAuthHandler
import com.dot.gallery.cloud.nextcloud.auth.NextcloudLoginFlowClient
import com.dot.gallery.cloud.webdav.WebDavMediaProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NextcloudModule {

    @Provides
    @Singleton
    fun provideNextcloudLoginFlowClient(): NextcloudLoginFlowClient = NextcloudLoginFlowClient()

    @Provides
    @Singleton
    @IntoSet
    fun provideNextcloudInteractiveAuthHandler(
        client: NextcloudLoginFlowClient
    ): CloudInteractiveAuthHandler = NextcloudInteractiveAuthHandler(client)

    @Provides
    @Singleton
    @IntoSet
    fun provideNextcloudProviderFactory(
        @ApplicationContext context: Context,
        cloudMediaDao: CloudMediaDao
    ): ProviderInstanceFactory = object : ProviderInstanceFactory {
        override val providerType = ProviderType.NEXTCLOUD
        override fun create(): MediaCapabilityProvider =
            WebDavMediaProvider(context, cloudMediaDao, NextcloudDialect())
    }
}
