/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.smart

import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import com.dot.gallery.feature_node.data.data_source.SmartScanDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SmartScanModule {
    @Provides
    @Singleton
    fun provideSmartScanDao(database: InternalDatabase): SmartScanDao = database.getSmartScanDao()

    @Provides
    @IntoSet
    fun provideSourceSyncProcessor(processor: SourceSyncProcessor): SmartScanPhaseProcessor = processor

    @Provides
    @IntoSet
    fun provideMetadataProcessor(processor: MetadataPhaseProcessor): SmartScanPhaseProcessor = processor

    @Provides
    @IntoSet
    fun provideSearchIndexProcessor(processor: SearchIndexPhaseProcessor): SmartScanPhaseProcessor = processor

    @Provides
    @IntoSet
    fun provideCategoryProcessor(processor: CategoryClassificationPhaseProcessor): SmartScanPhaseProcessor = processor

    @Provides
    @IntoSet
    fun provideFaceIndexProcessor(processor: FaceIndexPhaseProcessor): SmartScanPhaseProcessor = processor
}
