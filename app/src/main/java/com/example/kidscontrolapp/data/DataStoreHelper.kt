package com.example.kidscontrolapp.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit // <-- THIS IMPORT IS REQUIRED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "parent_prefs")

object DataStoreHelper {
    private val PARENT_ID_KEY = stringPreferencesKey("parent_id")

    // Save parentId to DataStore
    suspend fun saveParentId(context: Context, parentId: String) {
        context.dataStore.edit { prefs ->
            prefs[PARENT_ID_KEY] = parentId
        }
    }

    // Retrieve parentId from DataStore
    fun getParentId(context: Context): Flow<String?> =
        context.dataStore.data.map { prefs ->
            prefs[PARENT_ID_KEY]
        }
}
