package com.example.kidscontrolapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import com.example.kidscontrolapp.screens.*
import com.example.kidscontrolapp.viewmodel.DangerZoneViewModel
import java.net.URLDecoder

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val ENTER_UID = "enter_uid"
    const val DASHBOARD = "dashboard/{childUID}" // with argument
    const val INBOX = "inbox"
    const val PROFILE = "profile"
    const val CHILD_MAP = "child_map"
    const val DANGER_ZONE = "danger_zone"
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    // SINGLE shared ViewModel instance for danger zones
    val dangerZoneViewModel: DangerZoneViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) { SplashScreen(navController) }
        composable(Routes.LOGIN) { LoginScreen(navController) }
        composable(Routes.SIGNUP) { SignUpScreen(navController) }
        composable(Routes.ENTER_UID) { EnterChildUIDScreen(navController) }

        composable(
            route = Routes.DASHBOARD,
            arguments = listOf(navArgument("childUID") { type = NavType.StringType })
        ) { backStackEntry ->
            val childUID = backStackEntry.arguments?.getString("childUID")?.let {
                URLDecoder.decode(it, "UTF-8")
            } ?: ""

            if (childUID.isNotBlank()) {
                DashboardWithDrawer(
                    childUID = childUID,
                    navController = navController,
                    dangerZoneViewModel = dangerZoneViewModel // <-- pass the shared VM
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Invalid Child UID", color = Color.Red)
                }
            }
        }

        composable(Routes.INBOX) { InboxScreen(navController) }
        composable(Routes.PROFILE) { ProfileScreen(navController) }
        composable(Routes.CHILD_MAP) { ChildMapScreen(navController) }

        // Danger Zone Route
        composable(Routes.DANGER_ZONE) {
            // Use the SAME shared ViewModel instance
            DangerZoneScreen(navController, viewModel = dangerZoneViewModel)
        }
    }
}


