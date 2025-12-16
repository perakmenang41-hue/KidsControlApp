package com.example.kidscontrolapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// ---------------------------------------------------------------------
// 1️⃣ Your custom colour palettes (feel free to edit the hex values)
// ---------------------------------------------------------------------
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    // You can override any other colour here if you want a completely
    // custom dark look (background, surface, onPrimary, …)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    // Uncomment / add more overrides if you need a custom light look:
    // background = Color(0xFFFFFBFE),
    // surface = Color(0xFFFFFBFE),
    // onPrimary = Color.White,
    // onSecondary = Color.White,
    // onTertiary = Color.White,
    // onBackground = Color(0xFF1C1B1F),
    // onSurface = Color(0xFF1C1B1F),
)

/**
 * KidsControlAppTheme – the single source of truth for colours, typography
 * and shapes in the whole app.
 *
 * @param darkTheme   `true` → use DarkColorScheme, `false` → use LightColorScheme.
 * @param dynamicColor  If the device runs Android 12+ and `true`, we fallback to
 *                      the system “Material You” palettes (`dynamicDarkColorScheme`,
 *                      `dynamicLightColorScheme`). Otherwise we use the static palettes
 *                      defined above.
 * @param content    Your composables.
 */
@Composable
fun KidsControlAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),   // default = system setting
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // -------------------------------------------------------------
    // Resolve the colour scheme to use
    // -------------------------------------------------------------
    val colorScheme = when {
        // Android 12+ dynamic colour (Material You)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        // Our static dark palette
        darkTheme -> DarkColorScheme

        // Our static light palette
        else -> LightColorScheme
    }

    // -------------------------------------------------------------
    // Apply the colours (and typography) to the MaterialTheme
    // -------------------------------------------------------------
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}