package com.ivy.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Stores the encoded appearance settings; the format belongs to the UI layer that encodes it. */
class AppearanceDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    suspend fun get(): String? = dataStore.data.map { it[AppearanceKey] }.firstOrNull()

    suspend fun set(encoded: String) {
        dataStore.edit { it[AppearanceKey] = encoded }
    }

    companion object {
        private val AppearanceKey = stringPreferencesKey("appearance_v1")
    }
}
