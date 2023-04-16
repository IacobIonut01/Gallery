/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.injection

import android.content.Context
import com.dot.gallery.feature_node.data.data_source.MediaDatabase
import com.dot.gallery.feature_node.data.repository.MediaRepositoryCompatImpl
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMediaRepository(@ApplicationContext context: Context, database: MediaDatabase): MediaRepository {
        return MediaRepositoryCompatImpl(context, database.getMediaDao(), database.getAlbumDao())
    }

}
