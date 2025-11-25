package com.example.kidscontrolapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.kidscontrolapp.components.BottomBar


@Composable
fun ProfileScreen(navController: NavHostController) {
    Scaffold(
        bottomBar = { BottomBar(navController) } // removed currentRoute parameter
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(text = "Profile", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("User profile and settings will appear here.")
        }
    }
}
