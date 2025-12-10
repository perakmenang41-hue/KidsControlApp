package com.example.kidscontrolapp.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.tracking.TrackingManager
import com.example.kidscontrolapp.viewmodel.ChildLocationViewModel
import com.google.android.gms.location.*

class DangerZoneService : Service() {

    companion object {
        const val CHANNEL_ID = "dangerzone_service_channel"
        const val NOTIF_ID = 1337
        const val CHILD_UID = "CHILD_UID"  // ← Add this line
    }

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private val childLocationVM = ChildLocationViewModel()

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create().apply {
            interval = 2000L
            fastestInterval = 1000L
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val childUID = intent?.getStringExtra("CHILD_UID") ?: ""
        val parentId = intent?.getStringExtra("parentId") ?: ""

        startForeground(NOTIF_ID, buildNotification("Tracking child..."))
        startLocationUpdates()

        // start tracking loop
        TrackingManager.startTracking(childLocationVM, parentId, childUID)

        return START_STICKY
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        fused.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    childLocationVM.updateLocation(
                        lat = it.latitude,
                        lon = it.longitude,
                        speed = it.speed,
                        battery = 100
                    )
                }
            }
        }, mainLooper)
    }

    // ===============================
    // Notification helper functions
    // ===============================
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DangerZone Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KidsControl — Tracking")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        TrackingManager.stopTracking()
        fused.removeLocationUpdates(object : LocationCallback() {})
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
