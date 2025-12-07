package com.example.kidscontrolapp.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.example.kidscontrolapp.utils.FirestoreProvider
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun SignUpScreen(navController: NavHostController) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val sharedPrefs = context.getSharedPreferences("child_prefs", Context.MODE_PRIVATE)

    // Generate random Parent ID
    fun generateParentId(length: Int = 8): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }

    // Hash password
    fun hashPassword(password: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(password.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sign Up", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = country,
            onValueChange = { country = it },
            label = { Text("Country") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                // Validation
                if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                    Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (password != confirmPassword) {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                loading = true

                // Generate new parent ID
                val parentId = generateParentId()
                val hashedPassword = hashPassword(password)

                // Get FCM token
                FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                    val fcmToken = if (tokenTask.isSuccessful) tokenTask.result else ""

                    val db = FirestoreProvider.getFirestore()
                    val parentData = hashMapOf(
                        "parentId" to parentId,
                        "name" to name,
                        "email" to email,
                        "country" to country,
                        "phone" to phone,
                        "password" to hashedPassword,
                        "fcmToken" to fcmToken
                    )

                    db.collection("Parent_registered")
                        .document(parentId)
                        .set(parentData)
                        .addOnSuccessListener {
                            // Save parentUID in shared preferences
                            sharedPrefs.edit().putString("parentUID", parentId).apply()

                            loading = false
                            Toast.makeText(context, "Sign Up Successful! Parent ID: $parentId", Toast.LENGTH_LONG).show()

                            navController.navigate("login") {
                                popUpTo("signup") { inclusive = true }
                            }
                        }
                        .addOnFailureListener { e ->
                            loading = false
                            Toast.makeText(context, "Failed to save parent: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up")
        }

        if (loading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}
