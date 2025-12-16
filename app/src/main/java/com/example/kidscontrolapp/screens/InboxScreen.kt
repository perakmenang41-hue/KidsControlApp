package com.example.kidscontrolapp.screens

import android.util.Log
import androidx.compose.foundation.clickable                // needed for .clickable
import androidx.compose.foundation.layout.Arrangement       // ← missing import
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MoreVert   // overflow icon
import androidx.compose.material3.*                     // Material‑3 UI components
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue                // delegate helpers
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect          // ← missing import
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight          // FontWeight
import androidx.navigation.NavController
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.ui.inbox.InboxViewModel
import com.example.kidscontrolapp.ui.theme.KidsControlAppTheme
import com.example.kidscontrolapp.ui.theme.ThemeManager
import androidx.hilt.navigation.compose.hiltViewModel   // Hilt helper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(navController: NavController) {
    // -------------------------------------------------
    // 1️⃣ Dark‑mode state (unchanged)
    // -------------------------------------------------
    val isDark by ThemeManager.isDark.collectAsState()

    LaunchedEffect(isDark) {
        Log.d("InboxScreen", "Dark mode state: $isDark")
    }

    // -------------------------------------------------
    // 2️⃣ Obtain the Hilt‑provided ViewModel
    // -------------------------------------------------
    val viewModel: InboxViewModel = hiltViewModel()
    val messages by viewModel.messages.collectAsState()

    // -------------------------------------------------
    // 3️⃣ Theme wrapper (unchanged)
    // -------------------------------------------------
    KidsControlAppTheme(darkTheme = isDark) {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.title_inbox)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->

            if (messages.isEmpty()) {
                // -------------------------------------------------
                // 4️⃣ Empty‑inbox UI
                // -------------------------------------------------
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.text_no_new_messages))
                }
            } else {
                // -------------------------------------------------
                // 5️⃣ List of real inbox messages
                // -------------------------------------------------
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ListItem(
                            // ---- Material‑3 parameter names ----
                            headlineContent = {
                                Text(
                                    "${msg.childName} is ${msg.status.lowercase()} ${msg.zoneName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (msg.status == "APPROACHING")
                                        FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            supportingContent = { Text(msg.readableTime) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { /* overflow menu – optional */ }) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = null
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* open detail screen if desired */ }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}