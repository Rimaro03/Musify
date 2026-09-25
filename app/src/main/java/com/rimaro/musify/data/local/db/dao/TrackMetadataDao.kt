package com.rimaro.musify.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.rimaro.musify.data.local.db.entity.TrackMetadata

@Dao
interface TrackMetadataDao {

    @Query("SELECT * FROM track_metadata WHERE trackId == :trackId")
    suspend fun getById(trackId: String): TrackMetadata?

    @Query("SELECT * FROM track_metadata WHERE trackId IN (:trackIds)")
    suspend fun getByIds(trackIds: List<String>): List<TrackMetadata>

    @Upsert
    suspend fun upsert(trackMetadata: TrackMetadata)

    @Upsert
    suspend fun upsertAll(trackMetadataList: List<TrackMetadata>)
}