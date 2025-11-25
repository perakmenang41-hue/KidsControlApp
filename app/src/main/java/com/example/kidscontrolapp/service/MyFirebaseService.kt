package com.example.kidscontrolapp.service

class MyFirebaseService : FirebaseMessagingService() {

    override fun onNewToken(fcmToken: String) {
        super.onNewToken(fcmToken)
        // This is where you send the token to your backend
        sendTokenToServer(fcmToken)
    }

    private fun sendTokenToServer(fcmToken: String) {
        val json = JSONObject()
        json.put("parentId", "95CVMGOC") // Replace with actual parent UID
        json.put("fcmToken", fcmToken)

        // Use Retrofit or OkHttp here
        // Example with OkHttp:
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://your-server-url/api/parent/save-token") // Use Render URL
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
}
