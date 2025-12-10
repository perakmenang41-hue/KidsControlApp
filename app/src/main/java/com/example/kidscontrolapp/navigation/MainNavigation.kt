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
    const val DANGER_ZONE = "danger_zone/{parentId}/{childUID}"
    const val INSIGHTS = "insights/{parentId}/{childUid}" // ⭐ ADDED
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    val dangerZoneViewModel: DangerZoneViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) { SplashScreen(navController) }
        composable(Routes.LOGIN) { LoginScreen(navController) }
        composable(Routes.SIGNUP) { SignUpScreen(navController) }
        composable(Routes.PARENT_CHOICE) { ParentChoiceScreen(navController) }
        composable(Routes.ENTER_UID) { EnterChildUIDScreen(navController) }

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

        composable(Routes.INBOX) { InboxScreen(navController) }
        composable(Routes.PROFILE) { ProfileScreen(navController) }
        composable(Routes.CHILD_MAP) { ChildMapScreen(navController) }

        composable(
            route = Routes.DANGER_ZONE,
            arguments = listOf(
                navArgument("parentId") { type = NavType.StringType },
                navArgument("childUID") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val parentId = backStackEntry.arguments?.getString("parentId") ?: ""
            val childUID = backStackEntry.arguments?.getString("childUID") ?: ""

            if (parentId.isNotBlank() && childUID.isNotBlank()) {
                DangerZoneScreen(
                    navController = navController,
                    viewModel = dangerZoneViewModel,
                    parentId = parentId,
                    childUid = childUID
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Parent ID or Child UID not found", color = Color.Red)
                }
            }
        }

        // ⭐ INSERTED HERE (INSIGHTS SCREEN)
        composable(
            route = Routes.INSIGHTS,
            arguments = listOf(
                navArgument("parentId") { type = NavType.StringType },
                navArgument("childUid") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val parentId = backStackEntry.arguments?.getString("parentId") ?: ""
            val childUid = backStackEntry.arguments?.getString("childUid") ?: ""

            ChildSafetyInsightsScreen(
                navController = navController,
                parentId = parentId,
                childUid = childUid
            )
        }
    }
}
