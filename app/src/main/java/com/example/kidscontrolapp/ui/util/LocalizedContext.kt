package com.example.kidscontrolapp.ui.util

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.kidscontrolapp.viewmodel.LocaleViewModel
import java.util.Locale

@Composable
fun rememberLocalizedContext(
    localeViewModel: LocaleViewModel
): Context {
    val baseContext = LocalContext.current
    val locale = localeViewModel.locale.collectAsState().value

    return remember(locale) {
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        baseContext.createConfigurationContext(config)
    }
}
