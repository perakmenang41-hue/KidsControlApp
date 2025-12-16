package com.example.kidscontrolapp.viewmodel

import androidx.compose.runtime.staticCompositionLocalOf

// This will hold your LocaleViewModel across the Compose tree
val LocalViewModel = staticCompositionLocalOf<LocaleViewModel> {
    error("LocaleViewModel not provided")
}
