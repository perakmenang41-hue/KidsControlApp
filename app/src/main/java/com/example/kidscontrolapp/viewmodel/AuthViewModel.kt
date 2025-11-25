package com.example.kidscontrolapp.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AuthViewModel : ViewModel() {
    val email = mutableStateOf("")
    val password = mutableStateOf("")
    val loading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    fun login(onSuccess: () -> Unit) {
        if (email.value.isBlank() || password.value.isBlank()) {
            errorMessage.value = "Email & Password required"
            return
        }
        loading.value = true
        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email.value, password.value)
            .addOnCompleteListener { task ->
                loading.value = false
                if (task.isSuccessful) onSuccess()
                else errorMessage.value = task.exception?.message
            }
    }

    fun signup(
        name: String,
        country: String,
        phone: String,
        confirmPassword: String,
        onSuccess: () -> Unit
    ) {
        if (password.value != confirmPassword) {
            errorMessage.value = "Passwords do not match"
            return
        }

        if (email.value.isBlank() || password.value.isBlank()) {
            errorMessage.value = "Email & Password required"
            return
        }

        loading.value = true
        FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(email.value, password.value)
            .addOnCompleteListener { task ->
                loading.value = false
                if (task.isSuccessful) {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
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
