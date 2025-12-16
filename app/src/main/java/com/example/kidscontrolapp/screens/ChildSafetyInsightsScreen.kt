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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.viewmodel.ChildInsightsViewModel
import com.example.kidscontrolapp.viewmodel.AlertItem
import com.example.kidscontrolapp.viewmodel.ZoneStateItem
import com.example.kidscontrolapp.viewmodel.toAgo
import com.example.kidscontrolapp.viewmodel.LocaleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildSafetyInsightsScreen(
    navController: NavController,
    parentId: String,
    childUid: String,
    localeViewModel: LocaleViewModel,
    viewModel: ChildInsightsViewModel = viewModel()
) {
    val context = LocalContext.current
    val locale by localeViewModel.locale.collectAsState()

    val localizedContext = remember(locale) {
        val config = context.resources.configuration
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    LaunchedEffect(Unit) {
        viewModel.start(parentId, childUid)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(localizedContext.getString(R.string.title_safety_insights)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = localizedContext.getString(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            SummaryCard(
                lat = viewModel.lat.value,
                lon = viewModel.lon.value,
                battery = viewModel.battery.value,
                speed = viewModel.speed.value,
                lastUpdated = viewModel.lastUpdated.value,
                localizedContext = localizedContext
            )

            Spacer(Modifier.height(16.dp))

            ZoneSection(viewModel.zoneStates, localizedContext)

            Spacer(Modifier.height(16.dp))

            AlertSection(viewModel.alerts, localizedContext)
        }
    }
}

/* ---------- Summary Card ---------- */
@Composable
fun SummaryCard(
    lat: Double?,
    lon: Double?,
    battery: Double?,
    speed: Double?,
    lastUpdated: String,
    localizedContext: android.content.Context
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(localizedContext.getString(R.string.label_last_updated, lastUpdated))
            Text(localizedContext.getString(R.string.label_lat, lat?.toString() ?: "--"))
            Text(localizedContext.getString(R.string.label_lon, lon?.toString() ?: "--"))
            Text(localizedContext.getString(R.string.label_battery, battery?.toString() ?: "--"))
            Text(localizedContext.getString(R.string.label_speed, speed?.toString() ?: "--"))
        }
    }
}

/* ---------- Zone Section ---------- */
@Composable
fun ZoneSection(zones: List<ZoneStateItem>, localizedContext: android.content.Context) {
    Text(localizedContext.getString(R.string.title_zone_states), style = MaterialTheme.typography.titleMedium)

    if (zones.isEmpty()) {
        Text(localizedContext.getString(R.string.msg_no_zones_recorded))
        return
    }

    Column {
        zones.forEach { zone ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(Modifier.padding(8.dp)) {
                    Text(zone.zoneId)
                    Text(localizedContext.getString(R.string.label_state, zone.state))
                    Text(localizedContext.getString(R.string.label_duration, zone.duration ?: "--"))
                }
            }
        }
    }
}

/* ---------- Alert Section ---------- */
@Composable
fun AlertSection(alerts: List<AlertItem>, localizedContext: android.content.Context) {
    Text(localizedContext.getString(R.string.title_recent_alerts), style = MaterialTheme.typography.titleMedium)

    if (alerts.isEmpty()) {
        Text(localizedContext.getString(R.string.msg_no_alerts))
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
