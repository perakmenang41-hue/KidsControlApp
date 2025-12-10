package com.example.kidscontrolapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kidscontrolapp.viewmodel.ChildInsightsViewModel
import com.example.kidscontrolapp.viewmodel.AlertItem
import com.example.kidscontrolapp.viewmodel.ZoneStateItem
import com.example.kidscontrolapp.viewmodel.toAgo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildSafetyInsightsScreen(
    navController: NavController,
    parentId: String,
    childUid: String,
    viewModel: ChildInsightsViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.start(parentId, childUid)
    }

    Scaffold(
        topBar = {
            // ✅ REAL WORKING BAR
            CenterAlignedTopAppBar(
                title = { Text("Safety Insights") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            SummaryCard(
                lat = viewModel.lat.value,
                lon = viewModel.lon.value,
                battery = viewModel.battery.value,
                speed = viewModel.speed.value,
                lastUpdated = viewModel.lastUpdated.value
            )

            Spacer(Modifier.height(16.dp))

            ZoneSection(viewModel.zoneStates)

            Spacer(Modifier.height(16.dp))

            AlertSection(viewModel.alerts)
        }
    }
}

@Composable
fun SummaryCard(lat: Double?, lon: Double?, battery: Double?, speed: Double?, lastUpdated: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Last Updated: $lastUpdated")
            Text("Lat: ${lat ?: "--"}")
            Text("Lon: ${lon ?: "--"}")
            Text("Battery: ${battery ?: "--"}%")
            Text("Speed: ${speed ?: "--"} m/s")
        }
    }
}

@Composable
fun ZoneSection(zones: List<ZoneStateItem>) {
    Text("Zone States", style = MaterialTheme.typography.titleMedium)

    if (zones.isEmpty()) {
        Text("No zones recorded.")
        return
    }

    Column {
        zones.forEach {
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(Modifier.padding(8.dp)) {
                    Text(it.zoneId)
                    Text("State: ${it.state}")
                    Text("Duration: ${it.duration ?: "--"}")
                }
            }
        }
    }
}

@Composable
fun AlertSection(alerts: List<AlertItem>) {
    Text("Recent Alerts", style = MaterialTheme.typography.titleMedium)

    if (alerts.isEmpty()) {
        Text("No alerts.")
        return
    }

    LazyColumn {
        items(alerts) { alert ->
            ListItem(
                leadingContent = {
                    Icon(Icons.Default.Warning, contentDescription = null)
                },
                headlineContent = {
                    Text(alert.message)
                },
                supportingContent = {
                    Text(alert.timestamp.toAgo())
                }
            )
        }
    }
}
