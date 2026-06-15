/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.di

import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.data.dao.CloudAlbumSyncDao
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.dao.CloudServerConfigDao
import com.dot.gallery.cloud.data.dao.CloudUploadPrefDao
import com.dot.gallery.cloud.data.dao.PersonDao
import com.dot.gallery.cloud.data.dao.SyncStateDao
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.cloud.data.repository.CloudRepositoryImpl
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CloudModule {

    @Multibinds
    abstract fun bindProviderSet(): Set<MediaCapabilityProvider>

    companion object {
        @Provides
        @Singleton
        fun provideCloudMediaDao(database: InternalDatabase): CloudMediaDao =
            database.getCloudMediaDao()

        @Provides
        @Singleton
        fun provideCloudServerConfigDao(database: InternalDatabase): CloudServerConfigDao =
            database.getCloudServerConfigDao()

        @Provides
        @Singleton
        fun providePersonDao(database: InternalDatabase): PersonDao =
            database.getPersonDao()

        @Provides
        @Singleton
        fun provideSyncStateDao(database: InternalDatabase): SyncStateDao =
            database.getSyncStateDao()

        @Provides
        @Singleton
        fun provideCloudAlbumSyncDao(database: InternalDatabase): CloudAlbumSyncDao =
            database.getCloudAlbumSyncDao()

        @Provides
        @Singleton
        fun provideCloudUploadPrefDao(database: InternalDatabase): CloudUploadPrefDao =
            database.getCloudUploadPrefDao()

        @Provides
        @Singleton
        fun provideCloudRepository(
            registry: ProviderRegistry,
            cloudMediaDao: CloudMediaDao
        ): CloudRepository = CloudRepositoryImpl(registry, cloudMediaDao)
    }
}
