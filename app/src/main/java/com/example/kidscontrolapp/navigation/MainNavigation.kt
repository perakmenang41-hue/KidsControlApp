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
import androidx.compose.material3.Text
import com.example.kidscontrolapp.screens.*
import com.example.kidscontrolapp.viewmodel.DangerZoneViewModel
import java.net.URLDecoder

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val PARENT_CHOICE = "parent_choice"
    const val ENTER_UID = "enter_uid"
    const val DASHBOARD = "dashboard/{childUID}/{parentId}"
    const val INBOX = "inbox"
    const val PROFILE = "profile"
    const val CHILD_MAP = "child_map"
    const val DANGER_ZONE = "danger_zone/{parentId}"
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    // SINGLE shared ViewModel for all danger zone screens
    val dangerZoneViewModel: DangerZoneViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        // Splash
        composable(Routes.SPLASH) { SplashScreen(navController) }

        // Login
        composable(Routes.LOGIN) { LoginScreen(navController) }

        // Signup
        composable(Routes.SIGNUP) { SignUpScreen(navController) }

        // Parent Choice
        composable(Routes.PARENT_CHOICE) { ParentChoiceScreen(navController) }

        // Enter UID
        composable(Routes.ENTER_UID) { EnterChildUIDScreen(navController) }

        // Dashboard with childUID and parentId arguments
        composable(
            route = Routes.DASHBOARD,
            arguments = listOf(
                navArgument("childUID") { type = NavType.StringType },
                navArgument("parentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val childUID = backStackEntry.arguments?.getString("childUID")?.let {
                URLDecoder.decode(it, "UTF-8")
            } ?: ""

            val parentId = backStackEntry.arguments?.getString("parentId")?.let {
                URLDecoder.decode(it, "UTF-8")
            } ?: ""

            if (childUID.isNotBlank() && parentId.isNotBlank()) {
                DashboardWithDrawer(
                    childUID = childUID,
                    navController = navController,
                    dangerZoneViewModel = dangerZoneViewModel,
                    parentId = parentId
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Invalid Child UID or Parent ID", color = Color.Red)
                }
            }
        }

        // Inbox
        composable(Routes.INBOX) { InboxScreen(navController) }

        // Profile
        composable(Routes.PROFILE) { ProfileScreen(navController) }

        // Child Map
        composable(Routes.CHILD_MAP) { ChildMapScreen(navController) }

        // Danger Zone (requires parentId)
        composable(
            route = Routes.DANGER_ZONE,
            arguments = listOf(navArgument("parentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val parentId = backStackEntry.arguments?.getString("parentId") ?: ""

            if (parentId.isNotBlank()) {
                DangerZoneScreen(
                    navController = navController,
                    viewModel = dangerZoneViewModel,
                    parentId = parentId
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Parent ID not found", color = Color.Red)
                }
            }
        }
    }
}
