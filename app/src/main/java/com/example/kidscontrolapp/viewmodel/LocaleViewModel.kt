package com.example.kidscontrolapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.kidscontrolapp.utils.LanguageDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import android.content.Context


class LocaleViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = LanguageDataStore(application)

    // Current selected Locale
    private val _locale = MutableStateFlow(Locale.getDefault())
    val locale: StateFlow<Locale> = _locale

    // Flag to trigger activity recreate
    private val _recreate = MutableStateFlow(false)
    val recreate: StateFlow<Boolean> = _recreate

    init {
        // Load saved language from DataStore
        viewModelScope.launch {
            dataStore.languageFlow.collect { savedLang ->
                if (!savedLang.isNullOrBlank()) {
                    val locale = if (savedLang == "system") Locale.getDefault() else Locale.forLanguageTag(savedLang)
                    _locale.value = locale
                    applyLocale(locale)
                }
            }
        }
    }

    /** Call this to switch language */
    fun selectLanguage(languageTag: String) {
        val locale = if (languageTag == "system") Locale.getDefault() else Locale.forLanguageTag(languageTag)

        // Apply locale immediately
        _locale.value = locale
        applyLocale(locale)

        // Save to DataStore and trigger UI recreate
        viewModelScope.launch {
            dataStore.saveLanguage(languageTag)
            _recreate.value = true

        }
    }

    /** Apply locale using AppCompatDelegate (modern approach) */
    private fun applyLocale(locale: Locale) {
        val languageTag = if (locale == Locale.getDefault()) "system" else locale.toLanguageTag()
        val appLocales = if (languageTag == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(appLocales)
    }

    /** Returns a Context with the current locale applied */
    fun getLocalizedContext(baseContext: Context): Context {
        val config = baseContext.resources.configuration
        config.setLocale(_locale.value)
        return baseContext.createConfigurationContext(config)
    }


}
