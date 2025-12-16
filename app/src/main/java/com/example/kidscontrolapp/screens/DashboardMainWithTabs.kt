package com.example.kidscontrolapp.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.ui.theme.ThemeManager
import com.example.kidscontrolapp.viewmodel.DangerZoneViewModel
import com.example.kidscontrolapp.viewmodel.LocaleViewModel
import com.example.kidscontrolapp.viewmodel.LocalViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DashboardMainWithTabs(
    navController: NavHostController,
    parentId: String,
    childUID: String,
    dangerZoneViewModel: DangerZoneViewModel,
    profileImageUri: Uri?,
    onProfileImageChange: (Uri?) -> Unit,
    selectedTab: String,
    onTabChange: (String) -> Unit
) {

    val localeViewModel: LocaleViewModel = viewModel()
    val localeVm = LocalViewModel.current

    val isDark by ThemeManager.isDark.collectAsState()
    val headerBackground = if (isDark) Color.Black else Color.White
    val headerContentColor = if (isDark) Color.White else Color.Black

    Box(modifier = Modifier.fillMaxSize()) {

        // ---------- HEADER ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(headerBackground)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = stringResource(R.string.content_desc_profile_avatar),
                tint = headerContentColor,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isDark) Color(0xFF444444) else Color(0xFFE0E0E0),
                        CircleShape
                    )
                    .padding(8.dp)
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = stringResource(R.string.label_dashboard),
                color = headerContentColor,
                style = MaterialTheme.typography.titleLarge
            )
        }

        // ---------- CONTENT ----------
        when (selectedTab) {
            "Dashboard" -> DashboardScreen(
                navController = navController,
                parentId = parentId,
                childUID = childUID,
                dangerZoneViewModel = dangerZoneViewModel,
                profileImageUri = profileImageUri,
                onProfileClick = {},
                onImagePick = onProfileImageChange,
                localeViewModel = localeViewModel
            )

            "Inbox" -> InboxScreen(navController)

            "Profile" -> ProfileScreen(
                navController = navController,
                profileImageUri = profileImageUri,
                onProfileImageChange = onProfileImageChange,
                localeViewModel = localeVm
            )
        }

        // ---------- BOTTOM NAV ----------
        DashboardBottomNav(
            selectedTab = selectedTab,
            onTabSelected = onTabChange,
            localeViewModel = localeVm,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
