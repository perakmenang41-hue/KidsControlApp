package com.example.kidscontrolapp.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.kidscontrolapp.navigation.Routes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun BottomBar(navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = { navController.navigate(Routes.INBOX) }) {
            Icon(Icons.Filled.Email, contentDescription = "Inbox")
        }
        IconButton(onClick = { navController.navigate(Routes.CHILD_MAP) }) {
            Icon(Icons.Filled.LocationOn, contentDescription = "Locate")
        }
        IconButton(onClick = { navController.navigate(Routes.PROFILE) }) {
            Icon(Icons.Filled.Person, contentDescription = "Profile")
        }
    }
}
