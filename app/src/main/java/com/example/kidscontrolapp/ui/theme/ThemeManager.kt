package com.example.kidscontrolapp.ui.theme

import android.content.Context
import com.example.kidscontrolapp.data.DataStoreHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ThemeManager – holds the current dark‑mode state for the whole app.
 *
 * • Call ThemeManager.init(context) once (e.g. in MainActivity) to start listening
 *   to the persisted value in DataStore.
 *
 * • Collect ThemeManager.isDark in your UI to get a Compose‑compatible State.
 *
 * • Call ThemeManager.toggle(context, enabled) from a Switch to change the theme.
 */
object ThemeManager {

    // Internal mutable state – updated from DataStore and from the UI.
    private val _isDark = MutableStateFlow(false)

    /** Public read‑only flow that UI composables can collect. */
    val isDark: StateFlow<Boolean> = _isDark

    /** Initialise – start listening to the DataStore value. */
    fun init(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            DataStoreHelper.isDarkModeEnabled(context).collect { enabled ->
                _isDark.value = enabled
            }
        }
    }

    /** Update the flag (UI + DataStore). */
    fun toggle(context: Context, enabled: Boolean) {
        _isDark.value = enabled          // instantly updates UI
        CoroutineScope(Dispatchers.IO).launch {
            DataStoreHelper.setDarkModeEnabled(context, enabled)
        }
    }
}