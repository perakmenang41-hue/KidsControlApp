package com.example.kidscontrolapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MyFirebaseService : FirebaseMessagingService() {

    // Called when a new FCM token is generated
    override fun onNewToken(fcmToken: String) {
        super.onNewToken(fcmToken)
        Log.d("FCM_TOKEN", "New FCM Token: $fcmToken")
        sendTokenToServer(fcmToken)
    }

    // Send FCM token to your backend
    private fun sendTokenToServer(fcmToken: String) {
        val json = JSONObject().apply {
            put("parentId", "95CVMGOC") // Replace with actual parent UID
            put("fcmToken", fcmToken)
        }

        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://kidscontrolapp.onrender.com/api/parent/save-token") // Replace with your backend URL
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FCM_TOKEN", "Failed to send token", e)
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("FCM_TOKEN", "Token sent successfully: ${response.body?.string()}")
            }
        })
    }

    // Called when a new FCM message is received
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Log the message for debugging
        Log.d("FCM_MESSAGE", "Message received: ${remoteMessage.data}")

        // Extract notification data
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Danger Zone Alert"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Your child is in or approaching a danger zone!"

        // Extract additional fields for debugging
        val parentId = remoteMessage.data["parentId"] ?: "unknown"
        val childUID = remoteMessage.data["childUID"] ?: "unknown"
        val zoneName = remoteMessage.data["zoneName"] ?: "unknown zone"
        val zoneState = remoteMessage.data["zoneState"] ?: "unknown" // outside, approach, inside, prolonged, exited

        Log.d("FCM_NOTIFICATION", "Parent: $parentId, Child: $childUID, Zone: $zoneName, State: $zoneState")

        // Show Toast safely on main thread
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                applicationContext,
                "$title: $body\nState: $zoneState",
                Toast.LENGTH_LONG
            ).show()
        }

        // Show a local notification
        showNotification(title, "$body\nState: $zoneState")
    }

    // Helper function to display local notifications
    private fun showNotification(title: String, body: String) {
        val channelId = "danger_zone_alerts"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Danger Zone Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        // Build and display the notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
