package com.example.kidscontrolapp.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import com.example.kidscontrolapp.viewmodel.ChildLocationHelper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.tracking.TrackingManager // <-- NEW IMPORT
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DangerZoneService : Service() {

    companion object {
        const val CHANNEL_ID = "dangerzone_service_channel"
        const val NOTIF_ID = 1337
        const val CHILD_UID = "CHILD_UID"
    }

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    // -------------------------------------------------
    // Inject the helper instead of the old ViewModel
    // -------------------------------------------------
    @Inject lateinit var childLocationHelper: ChildLocationHelper   // <-- NEW FIELD

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
        val childUID = intent?.getStringExtra(CHILD_UID) ?: ""
        val parentId = intent?.getStringExtra("parentId") ?: ""

        val defaultContent = getString(R.string.notif_default_content)

        startForeground(NOTIF_ID, buildNotification(defaultContent))
        startLocationUpdates()

        // Pass the helper to the tracking manager (unchanged API)
        TrackingManager.startTracking(childLocationHelper, parentId, childUID)

        return START_STICKY
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fused.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    childLocationHelper.updateLocation(
                        lat = it.latitude,
                        lon = it.longitude,
                        speed = it.speed,
                        battery = 100
                    )
                }
            }
        }, mainLooper)
    }

    // -------------------------------------------------
    // Notification helpers (unchanged)
    // -------------------------------------------------
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.notif_channel_name)
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE
            else 0
        )
        val title = getString(R.string.notif_title)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
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