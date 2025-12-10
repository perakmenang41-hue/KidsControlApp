package com.example.kidscontrolapp.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create the DataStore instance
private val Context.dataStore by preferencesDataStore(name = "parent_prefs")

object DataStoreHelper {

    // Keys
    private val PARENT_ID_KEY = stringPreferencesKey("parent_id")
    private val CHILD_UID_KEY = stringPreferencesKey("child_uid")

    // Save parentId and childUID
    suspend fun saveParentInfo(context: Context, parentId: String, childUID: String) {
        context.dataStore.edit { prefs ->
            prefs[PARENT_ID_KEY] = parentId
            prefs[CHILD_UID_KEY] = childUID
        }
    }

    // Get parentId
    fun getParentId(context: Context): Flow<String?> =
        context.dataStore.data.map { prefs ->
            prefs[PARENT_ID_KEY]
        }

    // Save childUID separately
    suspend fun saveChildUID(context: Context, childUID: String) {
        context.dataStore.edit { prefs ->
            prefs[CHILD_UID_KEY] = childUID
        }
    }

    // Get childUID
    fun getChildUID(context: Context): Flow<String?> =
        context.dataStore.data.map { prefs ->
            prefs[CHILD_UID_KEY]
        }

    // Clear all saved data (logout)
    suspend fun clearAll(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
