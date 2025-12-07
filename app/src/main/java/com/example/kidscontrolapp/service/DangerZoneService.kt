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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.model.DangerZone
import com.example.kidscontrolapp.utils.FirestoreProvider

class DangerZoneService : Service() {

    private val firestore = FirestoreProvider.getFirestore()
    private val alertedZones = mutableSetOf<String>()
    private var childUID: String? = null
    private var dangerZones: List<DangerZone> = emptyList()

    companion object {
        const val CHANNEL_ID = "danger_zone_channel"
        const val CHILD_UID = "child_uid"
        const val ZONES = "danger_zone"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("DangerZoneService", "onStartCommand called")

        try {
            childUID = intent?.getStringExtra(CHILD_UID)
            Log.d("DangerZoneService", "childUID=$childUID")

            dangerZones = intent?.getParcelableArrayListExtra<DangerZone>(ZONES) ?: emptyList()
            Log.d("DangerZoneService", "dangerZones.size=${dangerZones.size}")

            if (childUID.isNullOrBlank()) {
                Log.e("DangerZoneService", "Child UID is missing")
                stopSelf()
                return START_NOT_STICKY
            }

            createNotificationChannel()
            startForeground(1, createNotification("Tracking child..."))

            if (dangerZones.isEmpty()) {
                fetchDangerZonesFromFirestore()
                Log.d("DangerZoneService", "Fetching zones from Firestore")
            }

            listenChildLocation()
        } catch (e: Exception) {
            Log.e("DangerZoneService", "Error in onStartCommand", e)
            stopSelf()
        }

        return START_STICKY
    }

    private fun fetchDangerZonesFromFirestore() {
        firestore.collection("danger_zones")
            .get()
            .addOnSuccessListener { snapshot ->
                dangerZones = snapshot.documents.mapNotNull { doc ->
                    try {
                        DangerZone(
                            id = doc.id,
                            name = doc.getString("name") ?: "Unknown",
                            lat = doc.getDouble("lat") ?: 0.0,
                            lon = doc.getDouble("lon") ?: 0.0,
                            radius = doc.getDouble("radius") ?: 50.0
                        )
                    } catch (e: Exception) {
                        Log.e("DangerZoneService", "Error parsing zone", e)
                        null
                    }
                }
                Log.d("DangerZoneService", "Fetched ${dangerZones.size} danger zones")
            }
            .addOnFailureListener { e ->
                Log.e("DangerZoneService", "Failed to fetch danger zones", e)
            }
    }

    private fun listenChildLocation() {
        val uid = childUID ?: return
        firestore.collection("child_position")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DangerZoneService", "Firestore listener error", error)
                    return@addSnapshotListener
                }
                try {
                    val lat = snapshot?.getDouble("lat")
                    val lon = snapshot?.getDouble("lon")
                    if (lat != null && lon != null) {
                        checkZones(lat, lon)
                    }
                } catch (e: Exception) {
                    Log.e("DangerZoneService", "Error processing location update", e)
                }
            }
    }

    private fun checkZones(lat: Double, lon: Double) {
        if (dangerZones.isEmpty()) return

        dangerZones.forEach { zone ->
            try {
                val distance = FloatArray(1)
                Location.distanceBetween(lat, lon, zone.lat, zone.lon, distance)

                if (distance[0] <= zone.radius) {
                    if (!alertedZones.contains(zone.id)) {
                        sendAlert(zone)
                        alertedZones.add(zone.id)
                    }
                } else {
                    alertedZones.remove(zone.id)
                }
            } catch (e: Exception) {
                Log.e("DangerZoneService", "Error checking zone ${zone.id}", e)
            }
        }
    }

    private fun sendAlert(zone: DangerZone) {
        try {
            val notification = createNotification("ALERT! ${zone.name} zone breached!")
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(zone.id.hashCode(), notification)

            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        android.os.VibrationEffect.createOneShot(
                            500,
                            android.os.VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            }
        } catch (e: Exception) {
            Log.e("DangerZoneService", "Error sending alert", e)
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
            val channel =
                NotificationChannel(CHANNEL_ID, "Danger Zone Alerts", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
