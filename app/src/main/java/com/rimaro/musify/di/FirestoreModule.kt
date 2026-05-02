package com.rimaro.musify.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirestoreModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance().apply {
            // Enable offline persistence
            val settings = firestoreSettings {
                setLocalCacheSettings(persistentCacheSettings {  })
            }
            firestoreSettings = settings
        }
    }

    @Provides
    @Singleton
    fun provideFirestorePlaylistDao(
        firestore: FirebaseFirestore
    ): FirestorePlaylistDao {
        return FirestorePlaylistDao(firestore)
    }
}