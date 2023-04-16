/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.injection

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.dot.gallery.core.Settings
import com.dot.gallery.feature_node.data.data_source.MediaDatabase
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.use_case.AddMediaUseCase
import com.dot.gallery.feature_node.domain.use_case.GetAlbumsUseCase
import com.dot.gallery.feature_node.domain.use_case.GetMediaByAlbumUseCase
import com.dot.gallery.feature_node.domain.use_case.GetMediaByUriUseCase
import com.dot.gallery.feature_node.domain.use_case.GetMediaFavoriteUseCase
import com.dot.gallery.feature_node.domain.use_case.GetMediaTrashedUseCase
import com.dot.gallery.feature_node.domain.use_case.GetMediaUseCase
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Singleton
    fun provideSettings(@ApplicationContext context: Context): Settings {
        return Settings(context)
    }

    @Provides
    @Singleton
    fun provideMediaDatabase(app: Application): MediaDatabase {
        return Room.databaseBuilder(
            app,
            MediaDatabase::class.java,
            MediaDatabase.DATABASE
        )
            .allowMainThreadQueries()
            .build()
    }

    @Provides
    @Singleton
    fun provideMediaUseCases(repository: MediaRepository, settings: Settings): MediaUseCases {
        return MediaUseCases(
            addMediaUseCase = AddMediaUseCase(repository),
            getAlbumsUseCase = GetAlbumsUseCase(repository),
            getMediaUseCase = GetMediaUseCase(repository),
            getMediaByAlbumUseCase = GetMediaByAlbumUseCase(repository),
            getMediaFavoriteUseCase = GetMediaFavoriteUseCase(repository),
            getMediaTrashedUseCase = GetMediaTrashedUseCase(repository),
            getMediaByUriUseCase = GetMediaByUriUseCase(repository),
            mediaHandleUseCase = MediaHandleUseCase(repository, settings)
        )
    }
}
