package com.example.kidscontrolapp.system

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.kidscontrolapp.utils.FirestoreProvider

class LocationService : Service() {

    companion object {
        const val ACTION_START = "com.example.kidscontrolapp.START"
        const val ACTION_STOP = "com.example.kidscontrolapp.STOP"
        const val CHANNEL_ID = "location_service_channel"
    }

    private val firestore = FirestoreProvider.getFirestore()
    private var childUID: String = "child1" // or pass dynamically

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundService()
            ACTION_STOP -> stopForegroundService()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Child Tracking Service")
            .setContentText("Tracking child location in background")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(1, notification)

        // TODO: implement location updates here
        // Example: periodically push location to Firestore
    }

    private fun stopForegroundService() {
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
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

}
