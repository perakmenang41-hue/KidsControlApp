package com.example.kidscontrolapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey   // <-- existing import
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale        // <-- needed for default language

private val Context.dataStore by preferencesDataStore(name = "parent_prefs")

object DataStoreHelper {

    // -------------------------------------------------------------
    // Existing keys (keep them as‑is)
    // -------------------------------------------------------------
    private val PARENT_ID_KEY = stringPreferencesKey("parent_id")
    private val CHILD_UID_KEY = stringPreferencesKey("child_uid")

    // -------------------------------------------------------------
    // NEW – dark‑mode flag
    // -------------------------------------------------------------
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_enabled")

    // -------------------------------------------------------------
    // NEW – language key
    // -------------------------------------------------------------
    private const val LANGUAGE_KEY = "selected_language"

    // -------------------------------------------------------------
    // Existing save / get helpers (unchanged)
    // -------------------------------------------------------------
    suspend fun saveParentInfo(context: Context, parentId: String, childUID: String) {
        context.dataStore.edit { prefs ->
            prefs[PARENT_ID_KEY] = parentId
            prefs[CHILD_UID_KEY] = childUID
        }
    }

    fun getParentId(context: Context): Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[PARENT_ID_KEY] }

    suspend fun saveChildUID(context: Context, childUID: String) {
        context.dataStore.edit { prefs -> prefs[CHILD_UID_KEY] = childUID }
    }

    fun getChildUID(context: Context): Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[CHILD_UID_KEY] }

    // -------------------------------------------------------------
    // NEW – write / read dark‑mode flag
    // -------------------------------------------------------------
    /** Persist the user’s dark‑mode choice. */
    suspend fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[DARK_MODE_KEY] = enabled }
    }

    /** Flow that emits the current dark‑mode setting (default = false). */
    fun isDarkModeEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[DARK_MODE_KEY] ?: false }

    // -------------------------------------------------------------
    // NEW – write / read selected language
    // -------------------------------------------------------------
    /** Persist the user’s language choice (e.g. "en", "es", "fr"). */
    suspend fun saveLanguage(context: Context, languageCode: String) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(LANGUAGE_KEY)] = languageCode
        }
    }

    /** Flow that emits the stored language code, or the device default if none saved. */
    fun getLanguage(context: Context): Flow<String> = context.dataStore.data
        .map { prefs ->
            prefs[stringPreferencesKey(LANGUAGE_KEY)] ?: Locale.getDefault().language
        }

    // -------------------------------------------------------------
    // Existing clear‑all (logout)
    // -------------------------------------------------------------
    suspend fun clearAll(context: Context) {
        context.dataStore.edit { prefs -> prefs.clear() }
    }
}