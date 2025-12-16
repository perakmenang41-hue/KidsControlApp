package com.example.kidscontrolapp.system

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.example.kidscontrolapp.navigation.MainNavigation
import com.example.kidscontrolapp.ui.theme.KidsControlAppTheme
import com.example.kidscontrolapp.viewmodel.LocaleViewModel
import kotlinx.coroutines.delay
import dagger.hilt.android.AndroidEntryPoint   // <-- Hilt import

@AndroidEntryPoint                               // <-- Hilt entry point for this activity
class MainActivity : ComponentActivity() {

    private val localeViewModel: LocaleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            // Splash effect
            var keepSplash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                delay(2000)
                keepSplash = false
            }
            splashScreen.setKeepOnScreenCondition { keepSplash }

            // Observe current locale
            val locale by localeViewModel.locale.collectAsStateWithLifecycle()

            // Observe recreate flag
            val recreate by localeViewModel.recreate.collectAsStateWithLifecycle()

            // Restart Activity when language changes
            LaunchedEffect(recreate) {
                if (recreate) {
                    finishAffinity() // Close all previous activities
                    startActivity(intent) // Start fresh
                }
            }

            // ✅ Compose UI wrapped in theme with key(locale) to force recomposition
            KidsControlAppTheme {
                key(locale) { // Forces recomposition of the entire navigation graph when locale changes
                    MainNavigation(localeViewModel = localeViewModel)
                }
            }
        }
    }
}