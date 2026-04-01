package com.rimaro.musify.data.local.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTrackStatus(status: TrackStatus): String = status.name

    @TypeConverter
    fun toTrackStatus(value: String): TrackStatus = TrackStatus.valueOf(value)
}