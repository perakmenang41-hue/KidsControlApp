package com.example.kidscontrolapp.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.kidscontrolapp.data.DataStoreHelper
import com.example.kidscontrolapp.utils.FirestoreProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder
import java.util.*

@Composable
fun EnterChildUIDScreen(navController: NavController) {
    val context = LocalContext.current
    var childUID by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }

    val firestore = FirestoreProvider.getFirestore()
    val scope = rememberCoroutineScope()

    // ✅ State to track logged-in parentId
    val parentIdState = remember { mutableStateOf<String?>(null) }

    // Load parentId from DataStore
    LaunchedEffect(Unit) {
        DataStoreHelper.getParentId(context).collect { id ->
            parentIdState.value = id
        }
    }

    val parentId = parentIdState.value
    val isParentLoading = parentId.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Enter Child UID", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = childUID,
            onValueChange = { childUID = it },
            label = { Text("Child UID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isParentLoading) {
            Text("Loading parent info...")
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                if (childUID.isBlank()) {
                    Toast.makeText(context, "Please enter a UID", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (parentId.isNullOrBlank()) {
                    Toast.makeText(context, "Parent not loaded yet, wait a moment...", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isChecking = true

                scope.launch(Dispatchers.IO) {
                    try {
                        val uidToCheck = childUID.trim()

                        // 1️⃣ Check if child exists in Firestore
                        val childQuery = firestore.collection("registered_users")
                            .whereEqualTo("uniqueId", uidToCheck)
                            .get()
                            .await()

                        if (childQuery.isEmpty) {
                            launch(Dispatchers.Main) {
                                isChecking = false
                                Toast.makeText(context, "Invalid Child UID", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }

                        val childDoc = childQuery.documents[0]
                        val childId = childDoc.id
                        val childName = childDoc.getString("name") ?: "Unknown"

                        // 2️⃣ Check if already linked to this parent
                        val existingFamily = firestore.collection("family")
                            .document(childId)
                            .collection("members")
                            .document(parentId)
                            .get()
                            .await()

                        if (existingFamily.exists()) {
                            launch(Dispatchers.Main) {
                                isChecking = false
                                Toast.makeText(context, "Child already linked! Opening dashboard...", Toast.LENGTH_SHORT).show()

                                // ✅ Safe navigation
                                val encodedChildId = URLEncoder.encode(childId, "UTF-8")
                                val encodedParentId = URLEncoder.encode(parentId, "UTF-8")
                                navController.navigate("dashboard/$encodedChildId/$encodedParentId")
                            }
                            return@launch
                        }

                        // 3️⃣ Check if linked to any other parent
                        val otherFamily = firestore.collectionGroup("members")
                            .whereEqualTo("childId", childId)
                            .whereNotEqualTo("parentId", parentId)
                            .get()
                            .await()

                        if (!otherFamily.isEmpty) {
                            launch(Dispatchers.Main) {
                                isChecking = false
                                Toast.makeText(context, "Child is already linked to another parent.", Toast.LENGTH_LONG).show()
                            }
                            return@launch
                        }

                        // 4️⃣ Add child under this parent
                        firestore.collection("family")
                            .document(childId)
                            .collection("members")
                            .document(parentId)
                            .set(
                                mapOf(
                                    "parentId" to parentId,
                                    "childId" to childId,
                                    "childUID" to uidToCheck,
                                    "name" to childName,
                                    "createdAt" to Date()
                                )
                            )
                            .await()

                        launch(Dispatchers.Main) {
                            isChecking = false
                            Toast.makeText(context, "Child linked successfully!", Toast.LENGTH_SHORT).show()

                            // ✅ Safe navigation
                            val encodedChildId = URLEncoder.encode(childId, "UTF-8")
                            val encodedParentId = URLEncoder.encode(parentId, "UTF-8")
                            navController.navigate("dashboard/$encodedChildId/$encodedParentId")
                        }

                    } catch (e: Exception) {
                        launch(Dispatchers.Main) {
                            isChecking = false
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isChecking && !isParentLoading
        ) {
            Text(if (isChecking || isParentLoading) "Checking..." else "Add Child")
        }
    }
}
