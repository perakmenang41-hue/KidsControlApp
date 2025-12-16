package com.example.kidscontrolapp.components

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.model.DangerZone
import com.example.kidscontrolapp.network.DangerZoneRequest
import com.example.kidscontrolapp.network.DangerZoneResponse
import com.example.kidscontrolapp.network.GenericResponse
import com.example.kidscontrolapp.network.RetrofitClient
import com.example.kidscontrolapp.network.UpdateLocationRequest
import com.example.kidscontrolapp.viewmodel.DangerZoneViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun AddDangerZoneButton(
    parentId: String,
    childUID: String,
    childLat: Double,
    childLng: Double,
    zoneName: String = "New Zone",
    radiusText: String = "50",
    viewModel: DangerZoneViewModel,
    enabled: Boolean = true,
    modifier: Modifier = Modifier // Added modifier for positioning
) {
    val context = LocalContext.current
    val radius = radiusText.toDoubleOrNull() ?: 50.0
    var showDialog by remember { mutableStateOf(false) }

    FloatingActionButton(
        onClick = { if (enabled) showDialog = true },
        containerColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier // Apply the modifier here
    ) {
        Icon(
            painter = painterResource(id = R.drawable.add),
            contentDescription = "Add Danger Zone",
            modifier = Modifier.size(32.dp)
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Danger Zone") },
            text = { Text("Are you sure you want to add this danger zone?") },
            confirmButton = {
                Button(onClick = {
                    showDialog = false

                    if (parentId.isBlank()) {
                        Toast.makeText(context, "Parent ID missing!", Toast.LENGTH_LONG).show()
                        Log.e("AddDangerZone", "Parent ID empty, canceling.")
                        return@Button
                    }

                    if (childUID.isBlank()) {
                        Toast.makeText(context, "Child UID missing!", Toast.LENGTH_LONG).show()
                        Log.e("AddDangerZone", "Child UID empty, canceling.")
                        return@Button
                    }

                    if (zoneName.isBlank()) {
                        Toast.makeText(context, "Zone name cannot be blank.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (radius <= 0) {
                        Toast.makeText(context, "Radius must be > 0.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    addDangerZoneToBackend(parentId, zoneName, childLat, childLng, radius, context, viewModel)

                    val updateRequest = UpdateLocationRequest(
                        childUID = childUID,
                        lat = childLat,
                        lon = childLng,
                        speed = 0f,
                        battery = 100,
                        parentId = parentId
                    )
                    Log.d("AddZoneUpdate", "Sending child_position update: $updateRequest")

                    RetrofitClient.api.updateChildLocation(updateRequest)
                        .enqueue(object : Callback<GenericResponse> {
                            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                                if (response.isSuccessful) {
                                    Log.d("AddZoneUpdate", "Child position updated: ${response.body()}")
                                } else {
                                    Log.e("AddZoneUpdate", "Failed HTTP ${response.code()}: ${response.errorBody()?.string()}")
                                }
                            }

                            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                                Log.e("AddZoneUpdate", "Failed to update child position", t)
                            }
                        })

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

private fun addDangerZoneToBackend(
    parentId: String,
    name: String,
    lat: Double,
    lng: Double,
    radius: Double,
    context: Context,
    viewModel: DangerZoneViewModel,
    retry: Boolean = true
) {
    val request = DangerZoneRequest(parentId, name, lat, lng, radius)
    Log.d("AddDangerZone", "Sending backend request: $request")

    RetrofitClient.api.addDangerZone(request).enqueue(object : Callback<DangerZoneResponse> {
        override fun onResponse(call: Call<DangerZoneResponse>, response: Response<DangerZoneResponse>) {
            if (response.isSuccessful && response.body()?.success == true) {
                val zoneId = response.body()?.zoneId ?: return
                Toast.makeText(context, "Danger Zone added!", Toast.LENGTH_SHORT).show()
                viewModel.addZone(DangerZone(zoneId, name, lat, lng, radius))
                Log.d("AddDangerZone", "Zone added successfully to ViewModel: $zoneId")
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("AddDangerZone", "Backend rejected request: $errorBody")
                Toast.makeText(context, "Failed: ${response.body()?.message ?: errorBody ?: "Unknown"}", Toast.LENGTH_LONG).show()
            }
        }

        override fun onFailure(call: Call<DangerZoneResponse>, t: Throwable) {
            Log.e("AddDangerZone", "Network error", t)
            Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_LONG).show()
            if (retry && t is java.net.SocketTimeoutException) {
                Log.d("AddDangerZone", "Retrying backend request due to timeout...")
                addDangerZoneToBackend(parentId, name, lat, lng, radius, context, viewModel, retry = false)
            }
        }
    })
}
