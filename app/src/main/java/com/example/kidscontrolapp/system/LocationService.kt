package com.example.kidscontrolapp.system

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.kidscontrolapp.viewmodel.ChildLocationViewModel
import com.google.android.gms.location.*

class LocationService : Service() {

    companion object {
        const val ACTION_START = "com.example.kidscontrolapp.START"
        const val ACTION_STOP = "com.example.kidscontrolapp.STOP"
        const val CHANNEL_ID = "location_service_channel"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // 🔥 Now dynamic (received from Intent)
    private var parentId: String = ""
    private var childUid: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action == ACTION_START) {

            // Receive dynamic parentId + childUid
            parentId = intent.getStringExtra("parentId") ?: ""
            childUid = intent.getStringExtra("childUid") ?: ""

            startForegroundService()
        }

        if (intent?.action == ACTION_STOP) {
            stopForegroundService()
        }

        return START_STICKY
    }

    private fun startForegroundService() {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Child Tracking Service")
            .setContentText("Tracking child in background")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(1, notification)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).setMinUpdateDistanceMeters(1f).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (loc in result.locations) {
                    sendLocation(loc)
                }
            }
        }

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private val childLocationViewModel = ChildLocationViewModel()

    private fun sendLocation(location: Location, battery: Int = 100) {

        if (parentId.isBlank() || childUid.isBlank()) return

        childLocationViewModel.sendChildLocationUpdate(
            context = this,
            parentId = parentId,
            childUID = childUid,
            lat = location.latitude,
            lon = location.longitude,
            battery = battery,
            speed = location.speed
        )
    }

    private fun stopForegroundService() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}
