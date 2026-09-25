package com.rimaro.musify.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rimaro.musify.data.local.db.dao.TrackDao
import com.rimaro.musify.data.local.db.entity.CachedTrack

@Database(
    entities = [CachedTrack::class],
    version = 4,
    exportSchema = true
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
}