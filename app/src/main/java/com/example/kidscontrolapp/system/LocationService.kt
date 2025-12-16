package com.example.kidscontrolapp.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.viewmodel.ChildLocationHelper   // <-- NEW IMPORT
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    companion object {
        const val ACTION_START = "com.example.kidscontrolapp.START"
        const val ACTION_STOP  = "com.example.kidscontrolapp.STOP"
        const val CHANNEL_ID   = "location_service_channel"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var parentId: String = ""
    private var childUid: String = ""

    // -------------------------------------------------
    // Inject the helper (no longer a ViewModel)
    // -------------------------------------------------
    @Inject lateinit var childLocationHelper: ChildLocationHelper   // <-- NEW FIELD

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                parentId = intent.getStringExtra("parentId") ?: ""
                childUid = intent.getStringExtra("childUid") ?: ""
                launchForegroundService()
            }
            ACTION_STOP -> stopForegroundService()
        }
        return START_STICKY
    }

    // -------------------------------------------------
    // Helper that starts the foreground service
    // -------------------------------------------------
    private fun launchForegroundService() {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
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

    private fun sendLocation(location: Location, battery: Int = 100) {
        if (parentId.isBlank() || childUid.isBlank()) return

        childLocationHelper.sendChildLocationUpdate(
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