package com.example.kidscontrolapp.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.kidscontrolapp.network.RetrofitClient
import com.example.kidscontrolapp.network.UpdateLocationRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChildLocationViewModel : ViewModel() {

    // Existing function — DO NOT CHANGE
    fun sendChildLocationUpdate(
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
                    if (response.isSuccessful) {
                        Log.d("ChildLocationVM", "Location updated successfully")
                    } else {
                        Log.e("ChildLocationVM", "Error updating location: ${response.code()}")
                    }
                }

                override fun onFailure(
                    call: Call<com.example.kidscontrolapp.network.GenericResponse>,
                    t: Throwable
                ) {
                    Log.e("ChildLocationVM", "Failed to update location", t)
                }
            })
    }

    // ==========================
    // New: Live location holder
    // ==========================
    private val _lastLocation = MutableStateFlow<LocationData?>(null)
    val lastLocation: StateFlow<LocationData?> = _lastLocation

    data class LocationData(
        val lat: Double,
        val lon: Double,
        val speed: Float,
        val battery: Int
    )

    fun updateLocation(lat: Double, lon: Double, speed: Float, battery: Int) {
        _lastLocation.value = LocationData(lat, lon, speed, battery)
    }

    fun sendLocationToBackend(parentId: String, childUID: String) {
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
                    if (response.isSuccessful) Log.d("ChildLocationVM", "Location updated")
                    else Log.e("ChildLocationVM", "Backend error: ${response.code()}")
                }

                override fun onFailure(call: Call<com.example.kidscontrolapp.network.GenericResponse>, t: Throwable) {
                    Log.e("ChildLocationVM", "Failed to send location", t)
                }
            })
    }
}
