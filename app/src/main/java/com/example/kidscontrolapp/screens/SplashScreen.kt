package com.example.kidscontrolapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import com.example.kidscontrolapp.R
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun SplashScreen(navController: NavHostController) {

    // 🔒 Navigation guard (SURVIVES recomposition)
    var hasNavigated by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ktracker),
            contentDescription = "Splash Logo",
            modifier = Modifier.size(200.dp)
        )
    }

    LaunchedEffect(hasNavigated) {
        if (!hasNavigated) {
            hasNavigated = true
            delay(2000)
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}
