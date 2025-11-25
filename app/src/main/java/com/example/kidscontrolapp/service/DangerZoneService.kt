package com.example.kidscontrolapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.model.DangerZone
import com.google.firebase.firestore.FirebaseFirestore

class DangerZoneService : Service() {

    private val firestore = FirebaseFirestore.getInstance()
    private val alertedZones = mutableSetOf<Int>() // IDs as Int
    private var childUID: String? = null
    private var dangerZones: List<DangerZone> = emptyList()

    companion object {
        const val CHANNEL_ID = "danger_zone_channel"
        const val CHILD_UID = "child_uid"
        const val ZONES = "danger_zones"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        childUID = intent?.getStringExtra(CHILD_UID)

        // Explicit type casting for Serializable
        @Suppress("UNCHECKED_CAST")
        dangerZones = intent?.getSerializableExtra(ZONES) as? List<DangerZone> ?: emptyList()

        createNotificationChannel()
        startForeground(1, createNotification("Tracking child..."))

        childUID?.let { uid ->
            firestore.collection("child_locations")
                .document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val lat = snapshot.getDouble("latitude") ?: return@addSnapshotListener
                        val lon = snapshot.getDouble("longitude") ?: return@addSnapshotListener
                        checkZones(lat, lon)
                    }
                }
        }

        return START_STICKY
    }

    private fun checkZones(lat: Double, lon: Double) {
        val location = Location("").apply {
            this.latitude = lat
            this.longitude = lon
        }

        dangerZones.forEach { zone ->
            val distance = FloatArray(1)
            Location.distanceBetween(lat, lon, zone.lat, zone.lon, distance)

            if (distance[0] <= zone.radius) {
                // Convert zone.id to Int if it's coming as String
                val zoneId = zone.id.toIntOrNull() ?: return@forEach
                if (!alertedZones.contains(zoneId)) {
                    sendAlert(zoneId, zone)
                    alertedZones.add(zoneId)
                }
            } else {
                val zoneId = zone.id.toIntOrNull() ?: return@forEach
                alertedZones.remove(zoneId)
            }
        }
    }

    private fun sendAlert(zoneId: Int, zone: DangerZone) {
        val notification = createNotification("ALERT! ${zone.name} zone breached!")
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(zoneId, notification)

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(
                    500,
                    android.os.VibrationEffect.DEFAULT_AMPLITUDE
                ))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Danger Zone Alert")
            .setContentText(content)
            .setSmallIcon(R.drawable.danger)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Danger Zone Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
