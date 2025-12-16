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
import com.example.kidscontrolapp.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.kidscontrolapp.ui.inbox.InboxHelper
import com.example.kidscontrolapp.data.InboxMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyFirebaseService : FirebaseMessagingService() {

    // -------------------------------------------------
    // 1️⃣ New token handling (unchanged)
    // -------------------------------------------------
    override fun onNewToken(fcmToken: String) {
        super.onNewToken(fcmToken)
        Log.d("FCM_TOKEN", "New FCM Token: $fcmToken")
        sendTokenToServer(fcmToken)
    }

    // -------------------------------------------------
    // 2️⃣ Inject the **InboxHelper** (plain class)
    // -------------------------------------------------
    @Inject lateinit var inboxHelper: InboxHelper

    // -------------------------------------------------
    // Existing token‑to‑server logic (unchanged)
    // -------------------------------------------------
    private fun sendTokenToServer(fcmToken: String) {
        val json = JSONObject().apply {
            put("parentId", "95CVMGOC") // TODO: replace with real parent UID
            put("fcmToken", fcmToken)
        }

        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://kidscontrolapp.onrender.com/api/parent/save-token")
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

    // -------------------------------------------------
    // 3️⃣ Message handling – store the inbound FCM data
    // -------------------------------------------------
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM_MESSAGE", "Message received: ${remoteMessage.data}")

        // -------------------------------------------------
        // Extract title & body – fallback to defaults from resources
        // -------------------------------------------------
        val defaultTitle = getString(R.string.default_fcm_title)
        val defaultBody = getString(R.string.default_fcm_body)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: defaultTitle

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: defaultBody

        // -------------------------------------------------
        // Additional data (for logging / debugging)
        // -------------------------------------------------
        val parentId = remoteMessage.data["parentId"] ?: "unknown"
        val childUID = remoteMessage.data["childUID"] ?: "unknown"
        val zoneName = remoteMessage.data["zoneName"] ?: "unknown zone"
        val zoneState = remoteMessage.data["zoneState"] ?: "unknown"

        Log.d(
            "FCM_NOTIFICATION",
            "Parent: $parentId, Child: $childUID, Zone: $zoneName, State: $zoneState"
        )

        // -------------------------------------------------
        // Build the **Room entity** – only the fields that exist
        // -------------------------------------------------
        val inboxMessage = InboxMessage(
            // `id` is auto‑generated, so we leave it as the default `0L`
            childName = remoteMessage.data["childName"] ?: "Unknown Child",
            status    = remoteMessage.data["status"]    ?: "UNKNOWN",
            zoneName  = zoneName,                       // we already have this value
            timestamp = System.currentTimeMillis(),
            isRead    = false
        )

        // -------------------------------------------------
        // Store the message in the local DB (background thread)
        // -------------------------------------------------
        CoroutineScope(Dispatchers.IO).launch {
            inboxHelper.storeRemoteMessage(inboxMessage)
        }

        // -------------------------------------------------
        // UI feedback – toast (main thread)
        // -------------------------------------------------
        Handler(Looper.getMainLooper()).post {
            val toastMsg = getString(
                R.string.toast_fcm_message,
                title,
                body,
                getString(R.string.label_state),
                zoneState
            )
            Toast.makeText(applicationContext, toastMsg, Toast.LENGTH_LONG).show()
        }

        // -------------------------------------------------
        // Show a local notification
        // -------------------------------------------------
        showNotification(title, "$body\n${getString(R.string.label_state)}: $zoneState")
    }

    // -------------------------------------------------
    // 4️⃣ Notification helper – unchanged
    // -------------------------------------------------
    private fun showNotification(title: String, body: String) {
        val channelId = "danger_zone_alerts"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.notif_channel_name)
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        // Build the notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Use a unique ID each time (timestamp‑based)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}