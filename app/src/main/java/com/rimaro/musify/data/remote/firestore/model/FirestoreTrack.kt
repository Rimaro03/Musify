package com.rimaro.musify.data.remote.firestore.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class FirestoreTrack (
    @DocumentId
    val documentId: String = "",
    val trackId: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)