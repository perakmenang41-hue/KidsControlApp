package com.example.kidscontrolapp.utils

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

fun registerParentPushToken(parentId: String) {
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
            Log.e("PushToken", "Fetching FCM registration token failed", task.exception)
            return@addOnCompleteListener
        }
        val token = task.result
        // Send token to your backend
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://kidscontrolapp.onrender.com/api/parent/save-token")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val json = JSONObject()
                json.put("parentId", parentId)
                json.put("token", token)

                conn.outputStream.use { it.write(json.toString().toByteArray()) }
                if (conn.responseCode == 200) {
                    Log.d("PushToken", "Token sent successfully")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
