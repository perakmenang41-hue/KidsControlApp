package com.example.kidscontrolapp.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object EnterUID : Screen("enter_uid")
    object Tracking : Screen("tracking")
    object Mail : Screen("mail")        // renamed from Inbox
    object Profile : Screen("profile")
}
