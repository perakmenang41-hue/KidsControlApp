package com.example.kidscontrolapp.viewmodel

import android.content.Context                     // <-- new import
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.kidscontrolapp.R               // <-- R for string IDs

class AuthViewModel : ViewModel() {

    val email = mutableStateOf("")
    val password = mutableStateOf("")
    val loading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    /** Login – uses Firebase Auth only */
    fun login(context: Context, onSuccess: () -> Unit) {
        // -----------------------------------------------------------------
        // 1️⃣ Validate input – use a localized string for the error message
        // -----------------------------------------------------------------
        if (email.value.isBlank() || password.value.isBlank()) {
            // “Email & Password required” → from strings.xml
            errorMessage.value = context.getString(R.string.error_email_password_required)
            return
        }

        loading.value = true
        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email.value, password.value)
            .addOnCompleteListener { task ->
                loading.value = false
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    // Preserve the backend error (it may be useful for debugging)
                    errorMessage.value = task.exception?.message
                }
            }
    }

    /** Sign‑up – creates a Firebase Auth user and writes extra profile data to Realtime DB */
    fun signup(
        context: Context,               // <-- receive Context here as well
        name: String,
        country: String,
        phone: String,
        confirmPassword: String,
        onSuccess: () -> Unit
    ) {
        // -----------------------------------------------------------------
        // 2️⃣ Password‑confirmation check – localized error
        // -----------------------------------------------------------------
        if (password.value != confirmPassword) {
            errorMessage.value = context.getString(R.string.error_passwords_do_not_match)
            return
        }

        // -----------------------------------------------------------------
        // 3️⃣ Email / password presence check – same string as login
        // -----------------------------------------------------------------
        if (email.value.isBlank() || password.value.isBlank()) {
            errorMessage.value = context.getString(R.string.error_email_password_required)
            return
        }

        loading.value = true
        FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(email.value, password.value)
            .addOnCompleteListener { task ->
                loading.value = false
                if (task.isSuccessful) {
                    // Store extra profile fields in the Realtime Database
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val userRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)

                    val userMap = mapOf(
                        "name" to name,
                        "email" to email.value,
                        "country" to country,
                        "phone" to phone
                    )

                    userRef.setValue(userMap).addOnCompleteListener {
                        onSuccess()
                    }
                } else {
                    errorMessage.value = task.exception?.message
                }
            }
    }
}