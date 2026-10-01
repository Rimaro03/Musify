package com.rimaro.musify.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.rimaro.musify.di.AppScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
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
                setLocalCacheSettings(persistentCacheSettings { })
            }
            firestoreSettings = settings
        }
    }

    @Provides
    @Singleton
    fun provideFirestorePlaylistRepo(
        firestore: FirebaseFirestore,
        @AppScope appScope: CoroutineScope
    ): FirestorePlaylistRepo {
        return FirestorePlaylistRepo(firestore, appScope)
    }

    @Provides
    @Singleton
    fun provideFirestoreLikedTracksRepo(
        firestore: FirebaseFirestore,
        @AppScope appScope: CoroutineScope
    ): FirestoreLikedTracksRepo {
        return FirestoreLikedTracksRepo(firestore, appScope)
    }
}