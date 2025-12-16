package com.example.kidscontrolapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.ui.theme.ThemeManager
import com.example.kidscontrolapp.viewmodel.LocaleViewModel

@Composable
fun DashboardBottomNav(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    localeViewModel: LocaleViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locale by localeViewModel.locale.collectAsState()
    val localizedContext = remember(locale) {
        val config = context.resources.configuration
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    val isDark by ThemeManager.isDark.collectAsState()
    val backgroundBrush = if (isDark) Brush.verticalGradient(listOf(Color.Black, Color.Black))
    else Brush.verticalGradient(listOf(Color.White, Color.White))
    val unselectedColor = if (isDark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(backgroundBrush)
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            // Dashboard
            NavigationBarItem(
                selected = selectedTab == "Dashboard",
                onClick = { onTabSelected("Dashboard") },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = localizedContext.getString(R.string.content_desc_dashboard),
                        tint = if (selectedTab == "Dashboard") MaterialTheme.colorScheme.primary else unselectedColor
                    )
                },
                label = {
                    Text(
                        text = localizedContext.getString(R.string.label_dashboard),
                        color = if (selectedTab == "Dashboard") MaterialTheme.colorScheme.primary else unselectedColor
                    )
                }
            )

            // Inbox
            NavigationBarItem(
                selected = selectedTab == "Inbox",
                onClick = { onTabSelected("Inbox") },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = localizedContext.getString(R.string.content_desc_inbox),
                        tint = if (selectedTab == "Inbox") MaterialTheme.colorScheme.primary else unselectedColor
                    )
                },
                label = {
                    Text(
                        text = localizedContext.getString(R.string.label_inbox),
                        color = if (selectedTab == "Inbox") MaterialTheme.colorScheme.primary else unselectedColor
                    )
                }
            )

            // Profile
            NavigationBarItem(
                selected = selectedTab == "Profile",
                onClick = { onTabSelected("Profile") },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = localizedContext.getString(R.string.content_desc_profile),
                        tint = if (selectedTab == "Profile") MaterialTheme.colorScheme.primary else unselectedColor
                    )
                },
                label = {
                    Text(
                        text = localizedContext.getString(R.string.label_profile),
                        color = if (selectedTab == "Profile") MaterialTheme.colorScheme.primary else unselectedColor
                    )
                }
            )
        }
    }
}
