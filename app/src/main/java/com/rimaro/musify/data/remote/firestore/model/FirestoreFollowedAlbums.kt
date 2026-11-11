package com.rimaro.musify.data.remote.firestore.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class FirestoreFollowedAlbums (
    val albumId: Long = 0L,
    @ServerTimestamp val addedAt: Timestamp? = null
)