package com.example.kidscontrolapp.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.ui.theme.ThemeManager   // <- global dark‑mode flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    navController: NavHostController,
    profileImage: Uri?,                       // <- Uri? type
    onProfileClick: () -> Unit,               // opens drawer
    onImagePick: (Uri?) -> Unit,              // callback to update profile image
    showBackButton: Boolean = true
) {
    // -------------------------------------------------
    // 1️⃣ Read the global dark‑mode flag
    // -------------------------------------------------
    val isDark by ThemeManager.isDark.collectAsState()

    // -------------------------------------------------
    // 2️⃣ Choose colours based on the flag
    // -------------------------------------------------
    val backgroundColor = if (isDark) Color.Black else Color.White
    val contentColor = if (isDark) Color.White else Color.Black
    // Placeholder background for the avatar when no image is set
    val placeholderBg = if (isDark) Color(0xFF444444) else Color.LightGray.copy(alpha = 0.4f)

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            titleContentColor = contentColor,
            navigationIconContentColor = contentColor,
            actionIconContentColor = contentColor
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {

                // -------------------------------------------------
                // Avatar – either the real image or a placeholder
                // -------------------------------------------------
                if (profileImage != null) {
                    Image(
                        painter = rememberAsyncImagePainter(profileImage),
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onProfileClick() },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.user),
                        contentDescription = "Default Profile",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(placeholderBg)   // matches the current theme
                            .clickable { onProfileClick() }
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.back),
                        contentDescription = "Back"
                    )
                }
            } else {
                // Keep layout stable – an invisible spacer of the same size
                Spacer(modifier = Modifier.size(0.dp))
            }
        }
    )
}