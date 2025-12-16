package com.example.kidscontrolapp.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.model.DangerZone
import com.example.kidscontrolapp.viewmodel.DangerZoneViewModel
import com.example.kidscontrolapp.viewmodel.LocaleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteZonesScreen(
    navController: NavHostController,
    viewModel: DangerZoneViewModel,
    parentId: String,
    localeViewModel: LocaleViewModel
) {
    val context = LocalContext.current

    // 🌍 Observe language
    val locale by localeViewModel.locale.collectAsState()

    // 🌍 Localized context (same as Login / SignUp)
    val localizedContext = remember(locale) {
        val config = context.resources.configuration
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    // UI state
    val dangerZones by remember { derivedStateOf { viewModel.dangerZones.toList() } }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var zoneToDelete by remember { mutableStateOf<DangerZone?>(null) }

    // Load zones
    LaunchedEffect(parentId) {
        if (parentId.isNotBlank()) {
            viewModel.loadDangerZones(parentId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        localizedContext.getString(
                            R.string.title_manage_danger_zones
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = localizedContext.getString(
                                R.string.content_desc_back
                            )
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // Empty state
            if (dangerZones.isEmpty()) {
                Text(
                    text = localizedContext.getString(R.string.msg_no_zones_added),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dangerZones, key = { it.id }) { zone ->
                        ZoneRow(
                            zone = zone,
                            localizedContext = localizedContext
                        ) {
                            zoneToDelete = zone
                            showDeleteDialog = true
                        }
                    }
                }
            }

            // Delete confirmation dialog
            if (showDeleteDialog && zoneToDelete != null) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                        zoneToDelete = null
                    },
                    title = {
                        Text(
                            localizedContext.getString(
                                R.string.title_delete_danger_zone
                            )
                        )
                    },
                    text = {
                        Text(
                            localizedContext.getString(
                                R.string.msg_confirm_delete_zone,
                                zoneToDelete!!.name
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val zone = zoneToDelete ?: return@TextButton

                            viewModel.deleteDangerZone(
                                context = context,
                                parentId = parentId,
                                zoneId = zone.id,
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        localizedContext.getString(
                                            R.string.msg_zone_deleted,
                                            zone.name
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    showDeleteDialog = false
                                    zoneToDelete = null
                                },
                                onError = { message ->
                                    Toast.makeText(
                                        context,
                                        localizedContext.getString(
                                            R.string.msg_delete_failed,
                                            message
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    showDeleteDialog = false
                                }
                            )
                        }) {
                            Text(localizedContext.getString(R.string.btn_delete))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showDeleteDialog = false
                            zoneToDelete = null
                        }) {
                            Text(localizedContext.getString(R.string.btn_cancel))
                        }
                    }
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// Zone row
// ------------------------------------------------------------------
@Composable
fun ZoneRow(
    zone: DangerZone,
    localizedContext: Context,
    onDeleteClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = zone.name,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = localizedContext.getString(
                    R.string.label_radius,
                    zone.radius.toInt()
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = localizedContext.getString(
                    R.string.label_id,
                    zone.id
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.delete),
            contentDescription = localizedContext.getString(
                R.string.content_desc_delete
            ),
            modifier = Modifier
                .size(28.dp)
                .clickable { onDeleteClicked() }
        )
    }
}
