package com.example.kidscontrolapp.system

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.example.kidscontrolapp.utils.FirestoreProvider

class LocationApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firestore (real Firestore)
        val db = FirestoreProvider.getFirestore()

        // Log which Firestore is used
        Log.d("FIRESTORE_TEST", "Using Firestore host: ${db.firestoreSettings.host}")

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_channel",
                "Location Tracking",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
