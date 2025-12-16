package com.example.kidscontrolapp.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme

/**
 * Paints the whole composable with the current theme’s background colour.
 * Light → white (or your light gradient); Dark → the colour defined in
 * KidsControlAppTheme (black → pink, dark‑purple, etc.).
 */
@Composable
fun ThemedBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}