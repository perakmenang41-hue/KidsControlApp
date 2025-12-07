package com.example.kidscontrolapp.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DangerZoneAlertBox(status: String) {

    if (status == "OUTSIDE") return   // hide if outside

    val bgColor = when (status) {
        "APPROACHING" -> Color(0xFFF7B400)
        "INSIDE"      -> Color(0xFFD32F2F)
        "PROLONGED"   -> Color(0xFF8B0000)
        "EXITED"      -> Color(0xFF4CAF50)
        else          -> Color.Gray
    }

    val message = when (status) {
        "APPROACHING" -> "⚠️ Child is approaching a danger zone"
        "INSIDE"      -> "🚨 Child is INSIDE the danger zone!"
        "PROLONGED"   -> "⏱ Child stayed too long inside the zone"
        "EXITED"      -> "✅ Child exited the danger zone"
        else          -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bgColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Text(
                message,
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
