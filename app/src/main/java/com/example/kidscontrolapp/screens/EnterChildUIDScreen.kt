package com.example.kidscontrolapp.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun EnterChildUIDScreen(navController: NavController) {
    val context = LocalContext.current // Get current context for Toast and other Android operations
    var childUID by remember { mutableStateOf("") } // Holds the UID entered by the parent
    var isChecking by remember { mutableStateOf(false) } // Flag to prevent multiple checks at once

    val firestore = FirebaseFirestore.getInstance() // Firebase Firestore instance

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp) // Padding around the screen
    ) {
        // Screen title
        Text("Enter Child UID", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp)) // Space between title and input

        // Text input for parent to enter the child's UID
        OutlinedTextField(
            value = childUID, // Current value
            onValueChange = { childUID = it }, // Update state when user types
            label = { Text("Child UID") }, // Label shown above input
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp)) // Space before button

        // Start Tracking button
        Button(
            onClick = {
                // Check if the input field is empty
                if (childUID.isBlank()) {
                    Toast.makeText(context, "Please enter a UID", Toast.LENGTH_SHORT).show()
                    return@Button // Stop execution
                }

                isChecking = true // Disable button while checking Firestore

                // Query Firestore collection "registered_users" for the entered UID in uniqueId field
                firestore.collection("registered_users")
                    .whereEqualTo("uniqueId", childUID) // Query by uniqueId field
                    .get()
                    .addOnSuccessListener { result ->
                        isChecking = false // Re-enable button
                        if (!result.isEmpty) {
                            // UID exists → valid
                            Toast.makeText(context, "UID is valid! Opening Dashboard...", Toast.LENGTH_SHORT).show()

                            // Safely encode the UID to avoid navigation crashes
                            val safeUID = java.net.URLEncoder.encode(childUID, "UTF-8")
                            navController.navigate("dashboard/$safeUID")
                        } else {
                            // UID does not exist → invalid
                            Toast.makeText(context, "Invalid UID. Please check.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        isChecking = false // Re-enable button
                        Toast.makeText(context, "Error checking UID: ${e.message}", Toast.LENGTH_SHORT).show()
                    }


            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isChecking // Disable button if a check is in progress
        ) {
            // Change button text based on checking state
            Text(if (isChecking) "Checking..." else "Start Tracking")
        }
    }
}
