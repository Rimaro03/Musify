package com.rimaro.musify.di

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rimaro.musify.data.local.db.CachedTrack
import com.rimaro.musify.data.local.db.Converters
import com.rimaro.musify.data.local.db.TrackDao
import com.rimaro.musify.data.local.preferences.SearchHistoryManager
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
    fun provideSearchHistoryManager(
        @ApplicationContext context: Context
    ): SearchHistoryManager = SearchHistoryManager(context)

    @Database(
        entities = [CachedTrack::class],
        version = 1,
        exportSchema = true
    )
    @TypeConverters(Converters::class)
    abstract class AppDatabase : RoomDatabase() {
        abstract fun trackDao(): TrackDao
    }
}