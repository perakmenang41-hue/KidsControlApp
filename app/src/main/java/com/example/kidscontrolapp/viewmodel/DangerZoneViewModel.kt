package com.example.kidscontrolapp.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kidscontrolapp.model.DangerZone
import com.example.kidscontrolapp.network.DangerZoneRequest
import com.example.kidscontrolapp.network.DangerZoneResponse
import com.example.kidscontrolapp.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DangerZoneViewModel : ViewModel() {

    // State list of danger zones for Compose
    private val _dangerZones = mutableStateListOf<DangerZone>()
    val dangerZones: List<DangerZone> get() = _dangerZones

    // Add zone locally (used after successful backend call)
    fun addZone(zone: DangerZone) {
        _dangerZones.add(zone)
    }

    // Remove zone locally
    fun removeZone(zone: DangerZone) {
        _dangerZones.remove(zone)
    }

    // Fetch all zones for a parent from backend
    fun fetchZones(parentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Retrofit call: create dummy request if needed or add GET API later
                // Example: fetch all zones for parentId
                // Replace this with your actual GET API
                // Here we just clear & keep existing list for demonstration
                // _dangerZones.clear() // uncomment if fetching from backend
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Add zone to backend via Retrofit
    fun addZoneToBackend(
        parentId: String,
        name: String,
        lat: Double,
        lon: Double,
        radius: Double,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val request = DangerZoneRequest(
            parentId = parentId,
            name = name,
            latitude = lat,
            longitude = lon,
            radius = radius
        )

        RetrofitClient.api.addDangerZone(request).enqueue(object : Callback<DangerZoneResponse> {
            override fun onResponse(call: Call<DangerZoneResponse>, response: Response<DangerZoneResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    // Add zone locally
                    addZone(
                        DangerZone(
                            id = (_dangerZones.size + 1).toString(), // generate temporary id
                            name = name,
                            lat = lat,
                            lon = lon,
                            radius = radius
                        )
                    )
                    onSuccess()
                } else {
                    onFailure(response.body()?.message ?: "Unknown error")
                }
            }

            override fun onFailure(call: Call<DangerZoneResponse>, t: Throwable) {
                onFailure(t.message ?: "Network error")
            }
        })
    }
}
