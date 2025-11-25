package com.example.kidscontrolapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.kidscontrolapp.components.BottomBar


@Composable
fun InboxScreen(navController: NavHostController) {
    Scaffold(
        bottomBar = { BottomBar(navController) } // use BottomBar from navigation package
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(text = "Inbox", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Tracking started / stopped messages will appear here.")
        }
    }
}
