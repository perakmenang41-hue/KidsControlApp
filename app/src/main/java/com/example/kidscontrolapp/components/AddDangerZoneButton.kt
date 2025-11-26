package com.example.kidscontrolapp.components

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.kidscontrolapp.model.DangerZone
import com.example.kidscontrolapp.network.DangerZoneRequest
import com.example.kidscontrolapp.network.DangerZoneResponse
import com.example.kidscontrolapp.network.RetrofitClient
import com.example.kidscontrolapp.viewmodel.DangerZoneViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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

    // Log that the Composable is rendered
    LaunchedEffect(Unit) {
        Log.d("DANGER_ZONE", "AddDangerZoneButton Composable displayed")
    }

    // Convert radius safely
    val radius = radiusText.toDoubleOrNull() ?: 50.0

    Button(onClick = {
        Log.d("DANGER_ZONE", "AddDangerZoneButton clicked")
        addDangerZone(parentId, zoneName, childLat, childLng, radius, context, viewModel)
    }) {
        Text(text = "Add Danger Zone")
    }
}

private fun addDangerZone(
    parentId: String,
    name: String,
    lat: Double,
    lng: Double,
    radius: Double,
    context: Context,
    viewModel: DangerZoneViewModel
) {
    Log.d("DANGER_ZONE", "Preparing DangerZoneRequest")

    val request = DangerZoneRequest(parentId, name, lat, lng, radius)

    Log.d("DANGER_ZONE", "Sending addDangerZone request -> parentId=$parentId, name=$name, lat=$lat, lng=$lng, radius=$radius")

    RetrofitClient.api.addDangerZone(request).enqueue(object : Callback<DangerZoneResponse> {
        override fun onResponse(call: Call<DangerZoneResponse>, response: Response<DangerZoneResponse>) {
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d("DANGER_ZONE", "Danger zone added successfully: ${response.body()}")
                Toast.makeText(context, "Danger Zone Added!", Toast.LENGTH_SHORT).show()

                // Add zone to ViewModel locally
                viewModel.addZone(
                    DangerZone(
                        id = (viewModel.dangerZones.size + 1).toString(),
                        name = name,
                        lat = lat,
                        lon = lng,
                        radius = radius
                    )
                )
                Log.d("DANGER_ZONE", "Zone added to ViewModel: $name")
            } else {
                Log.e("DANGER_ZONE", "Failed to add danger zone: ${response.body()?.message ?: "Unknown error"}")
                Toast.makeText(
                    context,
                    "Failed: ${response.body()?.message ?: "Unknown error"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        override fun onFailure(call: Call<DangerZoneResponse>, t: Throwable) {
            Log.e("DANGER_ZONE", "Network error adding danger zone", t)
            Toast.makeText(context, "Failed: ${t.message ?: "Network error"}", Toast.LENGTH_SHORT).show()
        }
    })
}
