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
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.use_case.AddMediaUseCase
import com.dot.gallery.feature_node.domain.use_case.DeletePinnedAlbumUseCase
import com.dot.gallery.feature_node.domain.use_case.GetAlbumsUseCase
import com.dot.gallery.feature_node.domain.use_case.GetMediaByAlbumUseCase
import com.dot.gallery.feature_node.domain.use_case.GetMediaByUriUseCase
import com.dot.gallery.feature_node.domain.use_case.GetMediaFavoriteUseCase
import com.dot.gallery.feature_node.domain.use_case.GetMediaListByUrisUseCase
import com.dot.gallery.feature_node.domain.use_case.GetMediaTrashedUseCase
import com.dot.gallery.feature_node.domain.use_case.GetMediaUseCase
import com.dot.gallery.feature_node.domain.use_case.InsertPinnedAlbumUseCase
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
    fun provideDatabase(app: Application): InternalDatabase {
        return Room.databaseBuilder(app, InternalDatabase::class.java, InternalDatabase.NAME)
            .allowMainThreadQueries()
            .build()
    }

    @Provides
    @Singleton
    fun provideMediaUseCases(repository: MediaRepository, @ApplicationContext context: Context): MediaUseCases {
        return MediaUseCases(
            addMediaUseCase = AddMediaUseCase(repository),
            getAlbumsUseCase = GetAlbumsUseCase(repository),
            getMediaUseCase = GetMediaUseCase(repository),
            getMediaByAlbumUseCase = GetMediaByAlbumUseCase(repository),
            getMediaFavoriteUseCase = GetMediaFavoriteUseCase(repository),
            getMediaTrashedUseCase = GetMediaTrashedUseCase(repository),
            getMediaByUriUseCase = GetMediaByUriUseCase(repository),
            getMediaListByUrisUseCase = GetMediaListByUrisUseCase(repository),
            mediaHandleUseCase = MediaHandleUseCase(repository, context),
            insertPinnedAlbumUseCase = InsertPinnedAlbumUseCase(repository),
            deletePinnedAlbumUseCase = DeletePinnedAlbumUseCase(repository)
        )
    }
}
