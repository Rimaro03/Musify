package com.rimaro.musify.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SearchHistoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore by preferencesDataStore(name = "search_history")

    companion object {
        private val HISTORY_KEY = stringPreferencesKey("queries")
        private const val MAX_ENTRIES = 20
    }

    val history: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val json = prefs[HISTORY_KEY] ?: return@map emptyList()
        Json.Default.decodeFromString<List<String>>(json)
    }

    suspend fun add(query: String) {
        if (query.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[HISTORY_KEY]
                ?.let { Json.Default.decodeFromString<List<String>>(it) }
                ?.toMutableList() ?: mutableListOf()
            current.remove(query)       // remove if already exists
            current.add(0, query)       // re-add at top
            prefs[HISTORY_KEY] = Json.Default.encodeToString(current.take(MAX_ENTRIES))
        }
    }

    suspend fun remove(query: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[HISTORY_KEY]
                ?.let { Json.Default.decodeFromString<List<String>>(it) }
                ?.toMutableList() ?: return@edit
            current.remove(query)
            prefs[HISTORY_KEY] = Json.Default.encodeToString(current)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.remove(HISTORY_KEY) }
    }
}