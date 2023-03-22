package com.dot.gallery.injection

import android.content.ContentResolver
import android.content.Context
import com.dot.gallery.feature_node.data.repository.MediaRepositoryImpl
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.use_case.AddMediaUseCase
import com.dot.gallery.feature_node.domain.use_case.DeleteMediaUseCase
import com.dot.gallery.feature_node.domain.use_case.GetAlbumsUseCase
import com.dot.gallery.feature_node.domain.use_case.GetMediaByAlbumUseCase
import com.dot.gallery.feature_node.domain.use_case.GetMediaByIdUseCase
import com.dot.gallery.feature_node.domain.use_case.GetMediaUseCase
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
    fun provideMediaRepository(@ApplicationContext context: Context): MediaRepository {
        return MediaRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideMediaUseCases(repository: MediaRepository): MediaUseCases {
        return MediaUseCases(
            addMediaUseCase = AddMediaUseCase(repository),
            deleteMediaUseCase = DeleteMediaUseCase(repository),
            getAlbumsUseCase = GetAlbumsUseCase(repository),
            getMediaUseCase = GetMediaUseCase(repository),
            getMediaByAlbumUseCase = GetMediaByAlbumUseCase(repository),
            getMediaByIdUseCase = GetMediaByIdUseCase(repository)
        )
    }
}
