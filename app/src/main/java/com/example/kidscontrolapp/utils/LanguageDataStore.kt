package com.example.kidscontrolapp.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("language_prefs")

class LanguageDataStore(private val context: Context) {

    private val LANGUAGE_KEY = stringPreferencesKey("app_language")

    val languageFlow: Flow<String?> =
        context.dataStore.data.map { prefs ->
            prefs[LANGUAGE_KEY]
        }

    suspend fun saveLanguage(languageTag: String) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = languageTag
        }
    }
}
