package com.example.kidscontrolapp.viewmodel

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kidscontrolapp.utils.FirestoreProvider
import com.example.kidscontrolapp.data.DataStoreHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class LoginViewModel : ViewModel() {

    val loading: MutableState<Boolean> = mutableStateOf(false)
    val errorMessage: MutableState<String?> = mutableStateOf(null)

    private val db = FirestoreProvider.getFirestore()

    fun login(email: String, password: String, onSuccess: (parentId: String) -> Unit, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            // ✅ Update Compose state on main thread
            withContext(Dispatchers.Main) {
                loading.value = true
                errorMessage.value = null
            }

            try {
                val snapshot = db.collection("Parent_registered")
                    .whereEqualTo("email", email.trim())
                    .get()
                    .await()

                if (snapshot.isEmpty) {
                    withContext(Dispatchers.Main) {
                        loading.value = false
                        errorMessage.value = "Email not registered"
                    }
                    return@launch
                }

                val doc = snapshot.documents[0]
                val storedHashedPassword = doc.getString("password") ?: ""
                val parentId = doc.getString("parentId") ?: ""

                if (hashPassword(password) != storedHashedPassword) {
                    withContext(Dispatchers.Main) {
                        loading.value = false
                        errorMessage.value = "Incorrect password"
                    }
                    return@launch
                }

                // Save parentId
                DataStoreHelper.saveParentId(context, parentId)

                // Update FCM token
                FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                    val fcmToken = tokenTask.result ?: ""
                    if (fcmToken.isNotEmpty()) {
                        doc.reference.update("fcmToken", fcmToken)
                    }
                }

                withContext(Dispatchers.Main) {
                    loading.value = false
                    onSuccess(parentId)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.value = false
                    errorMessage.value = "Login failed: ${e.message}"
                }
            }
        }
    }

    // Move hashPassword outside login function
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}