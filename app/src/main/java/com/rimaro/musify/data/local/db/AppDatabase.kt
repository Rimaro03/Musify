package com.rimaro.musify.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rimaro.musify.data.local.db.dao.TrackAudioUrlDao
import com.rimaro.musify.data.local.db.dao.TrackMetadataDao
import com.rimaro.musify.data.local.db.entity.TrackAudioUrl
import com.rimaro.musify.data.local.db.entity.TrackMetadata

@Database(
    entities = [TrackAudioUrl::class, TrackMetadata::class],
    version = 7,
    exportSchema = true
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun trackAudioUrlDao(): TrackAudioUrlDao
    abstract fun trackMetadataDao(): TrackMetadataDao
}