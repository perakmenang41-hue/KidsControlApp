package com.example.kidscontrolapp.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.utils.FirestoreProvider
import com.example.kidscontrolapp.viewmodel.LocaleViewModel
import com.example.kidscontrolapp.ui.util.rememberLocalizedContext
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun SignUpScreen(
    navController: NavHostController,
    localeViewModel: LocaleViewModel
) {
    // ✅ Locale-aware context
    val context = rememberLocalizedContext(localeViewModel)

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val sharedPrefs = context.getSharedPreferences("child_prefs", Context.MODE_PRIVATE)

    fun generateParentId(length: Int = 8): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }

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

        Text(
            text = context.getString(R.string.title_sign_up),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(context.getString(R.string.label_full_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(context.getString(R.string.label_email)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = country,
            onValueChange = { country = it },
            label = { Text(context.getString(R.string.label_country)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(context.getString(R.string.label_phone)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(context.getString(R.string.label_password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(context.getString(R.string.label_confirm_password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_all_fields_required),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                if (password != confirmPassword) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_passwords_mismatch),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                loading = true
                val parentId = generateParentId()
                val hashedPassword = hashPassword(password)

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
                            sharedPrefs.edit().putString("parentUID", parentId).apply()
                            loading = false

                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_signup_success, parentId),
                                Toast.LENGTH_LONG
                            ).show()

                            navController.navigate("login") {
                                popUpTo("signup") { inclusive = true }
                            }
                        }
                        .addOnFailureListener { e ->
                            loading = false
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_signup_failed, e.message ?: "unknown"),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(context.getString(R.string.btn_sign_up))
        }

        if (loading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}
