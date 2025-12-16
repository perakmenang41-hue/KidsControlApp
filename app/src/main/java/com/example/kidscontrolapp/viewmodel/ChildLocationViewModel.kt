package com.example.kidscontrolapp.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.content.Context
import android.util.Log
import com.example.kidscontrolapp.network.RetrofitClient
import com.example.kidscontrolapp.network.UpdateLocationRequest
import com.example.kidscontrolapp.tracking.LocationHandler   // <-- interface import
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class ChildLocationViewModel @Inject constructor() : ViewModel(),
    LocationHandler {                               // <-- implements the interface

    // -----------------------------------------------------------------
    // Interface methods – `override` is required
    // -----------------------------------------------------------------
    override fun sendChildLocationUpdate(
        context: Context,
        parentId: String,
        childUID: String,
        lat: Double,
        lon: Double,
        battery: Int,
        speed: Float
    ) {
        val request = UpdateLocationRequest(
            parentId = parentId,
            childUID = childUID,
            lat = lat,
            lon = lon,
            battery = battery,
            speed = speed
        )
        RetrofitClient.api.updateChildLocation(request)
            .enqueue(object : Callback<com.example.kidscontrolapp.network.GenericResponse> {
                override fun onResponse(
                    call: Call<com.example.kidscontrolapp.network.GenericResponse>,
                    response: Response<com.example.kidscontrolapp.network.GenericResponse>
                ) {
                    if (response.isSuccessful) Log.d("ChildLocationViewModel", "Location updated")
                    else Log.e("ChildLocationViewModel", "Error ${response.code()}")
                }

                override fun onFailure(
                    call: Call<com.example.kidscontrolapp.network.GenericResponse>,
                    t: Throwable
                ) {
                    Log.e("ChildLocationViewModel", "Failed to update", t)
                }
            })
    }

    // -----------------------------------------------------------------
    // Live‑location holder (useful for UI)
    // -----------------------------------------------------------------
    private val _lastLocation = MutableStateFlow<LocationData?>(null)
    val lastLocation: StateFlow<LocationData?> = _lastLocation

    data class LocationData(
        val lat: Double,
        val lon: Double,
        val speed: Float,
        val battery: Int
    )

    override fun updateLocation(
        lat: Double,
        lon: Double,
        speed: Float,
        battery: Int
    ) {
        _lastLocation.value = LocationData(lat, lon, speed, battery)
    }

    // -----------------------------------------------------------------
    // Periodic backend push – required by LocationHandler
    // -----------------------------------------------------------------
    override fun sendLocationToBackend(parentId: String, childUID: String) {
        val loc = _lastLocation.value ?: return
        val request = UpdateLocationRequest(
            parentId = parentId,
            childUID = childUID,
            lat = loc.lat,
            lon = loc.lon,
            battery = loc.battery,
            speed = loc.speed
        )
        RetrofitClient.api.updateChildLocation(request)
            .enqueue(object : Callback<com.example.kidscontrolapp.network.GenericResponse> {
                override fun onResponse(
                    call: Call<com.example.kidscontrolapp.network.GenericResponse>,
                    response: Response<com.example.kidscontrolapp.network.GenericResponse>
                ) {
                    if (response.isSuccessful) Log.d("ChildLocationViewModel", "Backend updated")
                    else Log.e("ChildLocationViewModel", "Backend error ${response.code()}")
                }

                override fun onFailure(
                    call: Call<com.example.kidscontrolapp.network.GenericResponse>,
                    t: Throwable
                ) {
                    Log.e("ChildLocationViewModel", "Backend failure", t)
                }
            })
    }
}