package com.example.kidscontrolapp.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavHostController

fun currentRoute(navController: NavHostController): String? {
    val destination: NavDestination? = navController.currentDestination
    return destination?.route
}
