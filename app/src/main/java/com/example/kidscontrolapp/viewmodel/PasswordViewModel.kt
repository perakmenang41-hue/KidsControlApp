package com.example.kidscontrolapp.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PasswordViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _success = MutableStateFlow<Boolean?>(null)
    val success: StateFlow<Boolean?> = _success

    fun clearStatus() {
        _errorMessage.value = null
        _success.value = null
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        when {
            currentPassword.isBlank() -> {
                _errorMessage.value = "Enter current password"
                return
            }
            newPassword.isBlank() -> {
                _errorMessage.value = "Enter new password"
                return
            }
            newPassword != confirmPassword -> {
                _errorMessage.value = "Passwords do not match"
                return
            }
            newPassword.length < 6 -> {
                _errorMessage.value = "Password must be at least 6 characters"
                return
            }
        }

        _loading.value = true

        CoroutineScope(Dispatchers.IO).launch {
            val user = auth.currentUser
            if (user == null) {
                _loading.value = false
                _errorMessage.value = "User not logged in"
                return@launch
            }

            val credential = EmailAuthProvider.getCredential(
                user.email!!,
                currentPassword
            )

            user.reauthenticate(credential)
                .addOnCompleteListener { reauth ->
                    if (reauth.isSuccessful) {

                        user.updatePassword(newPassword)
                            .addOnCompleteListener { update ->
                                _loading.value = false

                                if (update.isSuccessful) {
                                    _success.value = true
                                } else {
                                    _errorMessage.value =
                                        update.exception?.localizedMessage
                                            ?: "Failed to update password"
                                }
                            }

                    } else {
                        _loading.value = false
                        _errorMessage.value =
                            reauth.exception?.localizedMessage
                                ?: "Current password is incorrect"
                    }
                }
        }
    }
}
