package com.example.kidscontrolapp.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun LoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    loading = true
                    FirebaseAuth.getInstance()
                        .signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            loading = false
                            if (task.isSuccessful) {

                                Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT).show()

                                val parentId = FirebaseAuth.getInstance().currentUser!!.uid

                                // ⭐ Fetch FCM token
                                FirebaseMessaging.getInstance().token
                                    .addOnCompleteListener { tokenTask ->
                                        if (!tokenTask.isSuccessful) {
                                            Log.w("FCM", "Fetching FCM registration token failed", tokenTask.exception)
                                            return@addOnCompleteListener
                                        }

                                        // retrieved token
                                        val fcmToken = tokenTask.result
                                        Log.d("FCM_TOKEN", fcmToken)

                                        // ⭐ Save token to backend
                                        GlobalScope.launch(Dispatchers.IO) {
                                            saveTokenToBackend(parentId, fcmToken)
                                        }
                                    }

                                // Navigate after login
                                navController.navigate("enter_uid") {
                                    popUpTo("login") { inclusive = true }
                                }

                            } else {
                                Toast.makeText(context, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Enter email & password", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Sign In") }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = { navController.navigate("signup") }) {
            Text("Don't have an account? Sign Up")
        }

        if (loading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}


// =====================================
// Save Token to Backend
// =====================================
fun saveTokenToBackend(parentId: String, token: String) {
    try {
        val url = URL("https:///10.0.2.2:5000/api/parent/save-token")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val jsonBody = """
        {
            "parentId": "$parentId",
            "fcmToken": "$token"
        }
        """

        connection.outputStream.write(jsonBody.toByteArray())
        val code = connection.responseCode
        Log.d("Backend", "Token Upload Response: $code")

    } catch (e: Exception) {
        Log.e("BackendError", "Error uploading token: ${e.message}")
    }
}
