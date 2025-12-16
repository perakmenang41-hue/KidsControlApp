package com.example.kidscontrolapp.system

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * This class is now **disabled** – it is not annotated with @HiltAndroidApp
 * and will not be used as the Application entry point.
 *
 * Keep the code here if you want to reactivate it later.
 */
internal class DisabledLocationApp : Application() {

    companion object {
        // You can still access this if you manually create an instance,
        // but it won’t be created automatically by the system.
        lateinit var firestore: FirebaseFirestore
    }

    override fun onCreate() {
        super.onCreate()

        // -------------------------------------------------
        // Initialise Firestore (original code – now optional)
        // -------------------------------------------------
        firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        // -------------------------------------------------
        // Create the notification channel (original code – now optional)
        // -------------------------------------------------
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