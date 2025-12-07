package com.example.kidscontrolapp.components

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.kidscontrolapp.utils.FirestoreProvider
import androidx.compose.ui.platform.LocalContext
import com.example.kidscontrolapp.model.DangerZone
import com.example.kidscontrolapp.viewmodel.DangerZoneViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@Composable
fun AddDangerZoneButton(
    parentId: String,
    childLat: Double,
    childLng: Double,
    zoneName: String = "New Zone",
    radiusText: String = "50",
    viewModel: DangerZoneViewModel
) {
    val context = LocalContext.current
    val radius = radiusText.toDoubleOrNull() ?: 50.0
    var showDialog by remember { mutableStateOf(false) }

    Button(onClick = { showDialog = true }) {
        Text(text = "Add Danger Zone")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm") },
            text = { Text("Are you sure you want to add this danger zone?") },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    addDangerZoneToFirestore(parentId, zoneName, childLat, childLng, radius, context, viewModel)
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("No")
                }
            }
        )
    }
}

private fun addDangerZoneToFirestore(
    parentId: String,
    name: String,
    lat: Double,
    lng: Double,
    radius: Double,
    context: Context,
    viewModel: DangerZoneViewModel
) {
    val db = FirestoreProvider.getFirestore()
    val dangerZoneId = UUID.randomUUID().toString() // unique ID for subcollection document

    val dangerZoneData = hashMapOf(
        "id" to dangerZoneId,
        "name" to name,
        "lat" to lat,
        "lon" to lng,
        "radius" to radius
    )

    db.collection("Parent_registered")
        .document(parentId)
        .collection("dangerZones")
        .document(dangerZoneId)
        .set(dangerZoneData)
        .addOnSuccessListener {
            Log.d("DANGER_ZONE", "Danger zone added under parent $parentId successfully")
            Toast.makeText(context, "Danger Zone Added!", Toast.LENGTH_SHORT).show()

            // Add to ViewModel so UI updates
            viewModel.addZone(
                DangerZone(
                    id = dangerZoneId,
                    name = name,
                    lat = lat,
                    lon = lng,
                    radius = radius
                )
            )
        }
        .addOnFailureListener { e ->
            Log.e("DANGER_ZONE", "Failed to add danger zone", e)
            Toast.makeText(context, "Failed to add Danger Zone: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}
