package com.example.kidscontrolapp.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.data.DataStoreHelper
import com.example.kidscontrolapp.navigation.Routes
import com.example.kidscontrolapp.ui.theme.ThemeManager
import com.example.kidscontrolapp.viewmodel.LocaleViewModel
import com.example.kidscontrolapp.viewmodel.ProfileHeaderViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    profileImageUri: Uri?,
    onProfileImageChange: (Uri?) -> Unit,
    localeViewModel: LocaleViewModel
) {
    val context: Context = LocalContext.current

    // ------------------- Localized context -------------------
    val currentLocale by localeViewModel.locale.collectAsState()
    val localizedContext: Context = remember(currentLocale) {
        localeViewModel.getLocalizedContext(context)
    }

    // ------------------- Image picker -------------------
    val imagePickerLauncher: ActivityResultLauncher<String> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri -> onProfileImageChange(uri) }

    // ------------------- Header ViewModel -------------------
    val headerVm: ProfileHeaderViewModel = viewModel()
    LaunchedEffect(Unit) {
        DataStoreHelper.getParentId(context).collect { pid ->
            pid?.let { headerVm.load(it) }
        }
    }
    val parentName by headerVm.parentName.collectAsState()

    // ------------------- Dark-mode flag -------------------
    val isDark by ThemeManager.isDark.collectAsState()
    var switchState by remember { mutableStateOf(isDark) }
    LaunchedEffect(isDark) { switchState = isDark }

    // ------------------- Background gradient -------------------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) listOf(Color.Black, Color(0xFFE0BBE4))
                    else listOf(Color(0xFFE0BBE4), Color.White)
                )
            )
    ) {
        Scaffold(
            topBar = { /* empty – we draw our own header */ },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {

                // ------------------- Profile Header -------------------
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    CircleShape
                                )
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileImageUri != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(profileImageUri),
                                    contentDescription = null,
                                    modifier = Modifier.size(74.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = parentName ?: localizedContext.getString(R.string.profile_username),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ------------------- Settings list -------------------
                SettingsCards(
                    navController = navController,
                    isDarkModeEnabled = switchState,
                    onSwitchChanged = { ThemeManager.toggle(context, it) },
                    localeViewModel = localeViewModel
                )
            }
        }
    }
}

@Composable
private fun SettingsCards(
    navController: NavController,
    isDarkModeEnabled: Boolean,
    onSwitchChanged: (Boolean) -> Unit,
    localeViewModel: LocaleViewModel
) {
    val context: Context = LocalContext.current
    val localizedContext: Context = remember { localeViewModel.getLocalizedContext(context) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        @Composable
        fun SettingsCard(
            title: String,
            icon: ImageVector,
            trailing: @Composable (() -> Unit)? = null,
            onClick: (() -> Unit)? = null
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onClick != null) { onClick?.invoke() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        title,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    when {
                        trailing != null -> trailing()
                        else -> Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        SettingsCard(
            title = localizedContext.getString(R.string.settings_profile_details),
            icon = Icons.Filled.AccountBox
        ) { navController.navigate(Routes.PROFILE_DETAILS) }

        SettingsCard(
            title = localizedContext.getString(R.string.settings_password),
            icon = Icons.Filled.Lock
        ) { navController.navigate(Routes.PASSWORD) }

        SettingsCard(
            title = localizedContext.getString(R.string.settings_dark_mode),
            icon = Icons.Filled.DarkMode,
            trailing = {
                Switch(
                    checked = isDarkModeEnabled,
                    onCheckedChange = { onSwitchChanged(it) },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        )

        SettingsCard(
            title = localizedContext.getString(R.string.settings_support),
            icon = Icons.Filled.SupportAgent
        ) { navController.navigate(Routes.SUPPORT) }

        SettingsCard(
            title = localizedContext.getString(R.string.settings_report_issue),
            icon = Icons.Filled.BugReport
        ) { navController.navigate(Routes.REPORT_ISSUE) }

        SettingsCard(
            title = localizedContext.getString(R.string.settings_about_app),
            icon = Icons.Filled.Info
        ) { navController.navigate(Routes.ABOUT_APP) }

        SettingsCard(
            title = localizedContext.getString(R.string.settings_language),
            icon = Icons.Filled.Language
        ) { navController.navigate(Routes.LANGUAGE) }

        SettingsCard(
            title = localizedContext.getString(R.string.settings_logout),
            icon = Icons.AutoMirrored.Filled.ExitToApp
        ) {
            navController.navigate(Routes.LOGIN) { popUpTo(Routes.LOGIN) { inclusive = true } }
        }
    }
}
