package com.example.kidscontrolapp.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kidscontrolapp.viewmodel.ChildLocationViewModel

@Composable
fun ChildZoneAlertScreen(
    childUID: String,
    viewModel: ChildLocationViewModel = viewModel()
) {
    val context = LocalContext.current

    // Start listening when Composable is shown
    DisposableEffect(childUID) {
        viewModel.startListening(childUID)
        onDispose {
            viewModel.stopListening()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Child Zone Alerts", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        // Show real-time alerts
        viewModel.lastAlert.value?.let { alert ->
            Text(alert, color = androidx.compose.ui.graphics.Color.Red)
            Toast.makeText(context, alert, Toast.LENGTH_SHORT).show()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Show zone states and durations
        viewModel.zoneStates.value.forEach { (zoneId, state) ->
            val duration = viewModel.timeInZone.value[zoneId] ?: "00:00:00:00"
            Text("Zone $zoneId: $state | Time: $duration")
        }
    }
}
