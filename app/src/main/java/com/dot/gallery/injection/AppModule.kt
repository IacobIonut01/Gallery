package com.dot.gallery.injection

import android.content.Context
import com.dot.gallery.feature_node.data.data_source.MediaDao
import com.dot.gallery.feature_node.data.data_source.MediaDaoImpl
import com.dot.gallery.feature_node.data.repository.MediaRepositoryImpl
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.use_case.AddMediaUseCase
import com.dot.gallery.feature_node.domain.use_case.DeleteMediaUseCase
import com.dot.gallery.feature_node.domain.use_case.GetMediaUseCase
import com.dot.gallery.feature_node.domain.use_case.GetMediaByAlbumUseCase
import com.dot.gallery.feature_node.domain.use_case.GetMediaByIdUseCase
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
    @Singleton
    fun provideMediaDao(@ApplicationContext context: Context): MediaDao {
        return MediaDaoImpl(context)
    }

    @Provides
    @Singleton
    fun provideMediaRepository(mediaDao: MediaDao): MediaRepository {
        return MediaRepositoryImpl(mediaDao)
    }

    @Provides
    @Singleton
    fun provideMediaUseCases(repository: MediaRepository): MediaUseCases {
        return MediaUseCases(
            addMediaUseCase = AddMediaUseCase(repository),
            deleteMediaUseCase = DeleteMediaUseCase(repository),
            getMediaUseCase = GetMediaUseCase(repository),
            getMediaByAlbumUseCase = GetMediaByAlbumUseCase(repository),
            getMediaByIdUseCase = GetMediaByIdUseCase(repository)
        )
    }
}
