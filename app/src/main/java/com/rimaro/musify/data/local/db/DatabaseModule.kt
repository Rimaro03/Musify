package com.rimaro.musify.data.local.db

import android.content.Context
import androidx.room.Room
import com.rimaro.musify.data.local.db.dao.TrackAudioUrlDao
import com.rimaro.musify.data.local.db.dao.TrackMetadataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "music_db"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun provideTrackAudioUrlDao(db: AppDatabase): TrackAudioUrlDao = db.trackAudioUrlDao()

    @Provides
    fun provideTrackMetadataDao(db: AppDatabase): TrackMetadataDao = db.trackMetadataDao()
}