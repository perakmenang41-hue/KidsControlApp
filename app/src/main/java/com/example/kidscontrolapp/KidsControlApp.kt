package com.example.kidscontrolapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KidsControlApp : Application() {

    companion object {
        /** Global Firestore instance you can reach via `KidsControlApp.firestore`. */
        lateinit var firestore: FirebaseFirestore
    }

    override fun onCreate() {
        super.onCreate()

        // -------------------------------------------------
        // Initialise Firestore (moved from LocationApp)
        // -------------------------------------------------
        firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        // -------------------------------------------------
        // Create the notification channel used by location services
        // -------------------------------------------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_channel",          // ID
                "Location Tracking",         // User‑visible name
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}