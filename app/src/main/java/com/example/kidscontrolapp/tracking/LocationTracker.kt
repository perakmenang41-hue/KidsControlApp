package com.example.kidscontrolapp.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.kidscontrolapp.network.ApiService
import com.example.kidscontrolapp.network.UpdateLocationRequest
import com.example.kidscontrolapp.network.GenericResponse
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LocationTracker(
    private val context: Context,
    private val api: ApiService,
    private val childUID: String,
    private val parentId: String
) {

    private val TAG = "LocationTracker"
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private var trackingJob: Job? = null

    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
        .setMinUpdateIntervalMillis(5000)
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                sendLocation(location)
            }
        }
    }

    fun startTracking() {
        // ✅ Use the context from constructor
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted && !coarseLocationGranted) {
            Log.e(TAG, "Location permission not granted")
            return
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            Log.d(TAG, "Started location tracking")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: location permission missing", e)
        }
    }

    fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        trackingJob?.cancel()
        Log.d(TAG, "Stopped location tracking")
    }

    private fun sendLocation(location: Location) {
        val request = UpdateLocationRequest(
            childUID = childUID,
            lat = location.latitude,
            lon = location.longitude,
            speed = location.speed,
            battery = getBatteryLevel(),
            parentId = parentId
        )

        api.updateChildLocation(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Location sent successfully")
                } else {
                    Log.e(TAG, "Failed to send location: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Log.e(TAG, "Error sending location", t)
            }
        })
    }

    private fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
