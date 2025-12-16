package com.example.kidscontrolapp.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.data.DataStoreHelper
import com.example.kidscontrolapp.utils.FirestoreProvider
import com.example.kidscontrolapp.viewmodel.LocaleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder
import java.util.*

@Composable
fun EnterChildUIDScreen(
    navController: NavController,
    localeViewModel: LocaleViewModel
) {
    val context = LocalContext.current
    val locale by localeViewModel.locale.collectAsState()

    val localizedContext = remember(locale) {
        val config = context.resources.configuration
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    var childUID by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }

    val firestore = FirestoreProvider.getFirestore()
    val scope = rememberCoroutineScope()

    val parentIdState = remember { mutableStateOf<String?>(null) }

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

        Text(
            text = localizedContext.getString(R.string.title_enter_child_uid),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = childUID,
            onValueChange = { childUID = it },
            label = { Text(localizedContext.getString(R.string.label_child_uid)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isParentLoading) {
            Text(text = localizedContext.getString(R.string.msg_loading_parent))
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                if (childUID.isBlank()) {
                    Toast.makeText(
                        localizedContext,
                        localizedContext.getString(R.string.toast_enter_uid),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                if (parentId.isNullOrBlank()) {
                    Toast.makeText(
                        localizedContext,
                        localizedContext.getString(R.string.toast_parent_not_loaded),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                isChecking = true

                scope.launch(Dispatchers.IO) {
                    try {
                        val uidToCheck = childUID.trim()

                        val childQuery = firestore.collection("registered_users")
                            .whereEqualTo("uniqueId", uidToCheck)
                            .get()
                            .await()

                        if (childQuery.isEmpty) {
                            launch(Dispatchers.Main) {
                                isChecking = false
                                Toast.makeText(
                                    localizedContext,
                                    localizedContext.getString(R.string.toast_invalid_uid),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@launch
                        }

                        val childDoc = childQuery.documents[0]
                        val childId = childDoc.id
                        val childName = childDoc.getString("name") ?: "Unknown"

                        val existingFamily = firestore.collection("family")
                            .document(childId)
                            .collection("members")
                            .document(parentId!!)
                            .get()
                            .await()

                        if (existingFamily.exists()) {
                            launch(Dispatchers.Main) {
                                isChecking = false
                                Toast.makeText(
                                    localizedContext,
                                    localizedContext.getString(R.string.toast_already_linked),
                                    Toast.LENGTH_SHORT
                                ).show()

                                val encChild = URLEncoder.encode(childId, "UTF-8")
                                val encParent = URLEncoder.encode(parentId!!, "UTF-8")
                                navController.navigate("dashboard_main/$encChild/$encParent")
                            }
                            return@launch
                        }

                        val otherFamily = firestore.collectionGroup("members")
                            .whereEqualTo("childId", childId)
                            .whereNotEqualTo("parentId", parentId!!)
                            .get()
                            .await()

                        if (!otherFamily.isEmpty) {
                            launch(Dispatchers.Main) {
                                isChecking = false
                                Toast.makeText(
                                    localizedContext,
                                    localizedContext.getString(R.string.toast_already_linked_other),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            return@launch
                        }

                        firestore.collection("family")
                            .document(childId)
                            .collection("members")
                            .document(parentId!!)
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
                            Toast.makeText(
                                localizedContext,
                                localizedContext.getString(R.string.toast_link_success),
                                Toast.LENGTH_SHORT
                            ).show()

                            val encChild = URLEncoder.encode(childId, "UTF-8")
                            val encParent = URLEncoder.encode(parentId!!, "UTF-8")
                            navController.navigate("dashboard/$encChild/$encParent")
                        }

                    } catch (e: Exception) {
                        launch(Dispatchers.Main) {
                            isChecking = false
                            Toast.makeText(
                                localizedContext,
                                localizedContext.getString(
                                    R.string.toast_error,
                                    e.message ?: "unknown"
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isChecking && !isParentLoading
        ) {
            Text(
                text = if (isChecking || isParentLoading)
                    localizedContext.getString(R.string.btn_checking)
                else
                    localizedContext.getString(R.string.btn_add_child)
            )
        }
    }
}
