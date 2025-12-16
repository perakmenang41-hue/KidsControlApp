package com.example.kidscontrolapp.navigation

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.data.DataStoreHelper
import com.example.kidscontrolapp.screens.*
import com.example.kidscontrolapp.viewmodel.DangerZoneViewModel
import com.example.kidscontrolapp.viewmodel.LocaleViewModel
import com.example.kidscontrolapp.viewmodel.LocalViewModel
import kotlinx.coroutines.flow.first
import java.net.URLDecoder

object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val PARENT_CHOICE = "parent_choice"
    const val ENTER_UID = "enter_uid"
    const val DASHBOARD_MAIN = "dashboard_main/{childUID}/{parentId}"
    const val PROFILE_DETAILS = "profile_details"
    const val PASSWORD = "password"
    const val INBOX = "inbox"
    const val CHILD_MAP = "child_map"
    const val REPORT_ISSUE = "report_issue"
    const val DANGER_ZONE = "danger_zone/{parentId}/{childUID}"
    const val INSIGHTS = "insights/{parentId}/{childUid}"
    const val DELETE_ZONES = "delete_zones/{parentId}"
    const val ABOUT_APP = "about_app"
    const val LANGUAGE = "language"
    const val SUPPORT = "support"
}

@Composable
fun MainNavigation(localeViewModel: LocaleViewModel) {

    val navController = rememberNavController()
    val context = LocalContext.current

    var dashboardSelectedTab by remember { mutableStateOf("Dashboard") }
    val dangerZoneViewModel: DangerZoneViewModel = viewModel()

    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var parentId by remember { mutableStateOf<String?>(null) }
    var childUID by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        parentId = DataStoreHelper.getParentId(context).first()
        childUID = DataStoreHelper.getChildUID(context).first()
    }

    CompositionLocalProvider(LocalViewModel provides localeViewModel) {

        Scaffold { paddingValues ->

            NavHost(
                navController = navController,
                startDestination = Routes.LOGIN,
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {

                composable(Routes.LOGIN) {
                    LoginScreen(navController, localeViewModel)
                }

                composable(Routes.SIGNUP) {
                    SignUpScreen(navController, localeViewModel)
                }

                composable(Routes.PARENT_CHOICE) {
                    ParentChoiceScreen(navController, localeViewModel)
                }

                composable(Routes.ENTER_UID) {
                    EnterChildUIDScreen(navController, localeViewModel)
                }

                composable(
                    route = Routes.DASHBOARD_MAIN,
                    arguments = listOf(
                        navArgument("childUID") { type = NavType.StringType },
                        navArgument("parentId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->

                    val uid = backStackEntry.arguments?.getString("childUID")
                        ?.let { URLDecoder.decode(it, "UTF-8") }
                    val pid = backStackEntry.arguments?.getString("parentId")
                        ?.let { URLDecoder.decode(it, "UTF-8") }

                    if (!uid.isNullOrBlank() && !pid.isNullOrBlank()) {
                        DashboardMainWithTabs(
                            navController = navController,
                            parentId = pid,
                            childUID = uid,
                            dangerZoneViewModel = dangerZoneViewModel,
                            profileImageUri = profileImageUri,
                            onProfileImageChange = { profileImageUri = it },
                            selectedTab = dashboardSelectedTab,
                            onTabChange = { dashboardSelectedTab = it }
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Loading Dashboard…")
                        }
                    }
                }

                composable(Routes.ABOUT_APP) {
                    AboutAppScreen(
                        navController = navController,
                        localeViewModel = localeViewModel,
                        onBack = {
                            dashboardSelectedTab = "Profile"
                            navController.popBackStack()
                        }
                    )
                }

                composable(Routes.PROFILE_DETAILS) {
                    ProfileDetailsScreen(navController, localeViewModel)
                }

                composable(Routes.PASSWORD) {
                    PasswordScreen(navController, localeViewModel)
                }

                composable(Routes.INBOX) {
                    InboxScreen(navController)
                }

                composable(Routes.CHILD_MAP) {
                    ChildMapScreen(navController)
                }

                composable(Routes.SUPPORT) {
                    SupportScreen(navController)
                }

                composable(Routes.REPORT_ISSUE) {
                    ReportIssueScreen(navController, localeViewModel)
                }

                composable(Routes.LANGUAGE) {
                    LanguageScreen(navController, localeViewModel)
                }

                composable(
                    route = Routes.DANGER_ZONE,
                    arguments = listOf(
                        navArgument("parentId") { type = NavType.StringType },
                        navArgument("childUID") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    DangerZoneScreen(
                        navController = navController,
                        viewModel = dangerZoneViewModel,
                        parentId = backStackEntry.arguments?.getString("parentId") ?: "",
                        childUid = backStackEntry.arguments?.getString("childUID") ?: "",
                        localeViewModel = localeViewModel
                    )
                }

                composable(
                    route = Routes.DELETE_ZONES,
                    arguments = listOf(navArgument("parentId") { type = NavType.StringType })
                ) { backStackEntry ->
                    DeleteZonesScreen(
                        navController = navController,
                        viewModel = dangerZoneViewModel,
                        parentId = backStackEntry.arguments?.getString("parentId").orEmpty(),
                        localeViewModel = localeViewModel
                    )
                }

                composable(
                    route = Routes.INSIGHTS,
                    arguments = listOf(
                        navArgument("parentId") { type = NavType.StringType },
                        navArgument("childUid") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    ChildSafetyInsightsScreen(
                        navController = navController,
                        parentId = backStackEntry.arguments?.getString("parentId") ?: "",
                        childUid = backStackEntry.arguments?.getString("childUid") ?: "",
                        localeViewModel = localeViewModel
                    )
                }
            }
        }
    }
}
