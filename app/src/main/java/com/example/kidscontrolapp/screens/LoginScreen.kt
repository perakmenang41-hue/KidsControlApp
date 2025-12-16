package com.example.kidscontrolapp.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource          // <-- added
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kidscontrolapp.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.kidscontrolapp.viewmodel.LocaleViewModel
import com.example.kidscontrolapp.viewmodel.LoginViewModel
import com.example.kidscontrolapp.data.DataStoreHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.kidscontrolapp.navigation.Routes

@Composable
fun LoginScreen(
    navController: NavHostController,
    localeViewModel: LocaleViewModel,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val locale by localeViewModel.locale.collectAsState()

    // Create a localized context using the current locale
    val localizedContext = remember(locale) {
        val config = context.resources.configuration
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title – localized
        Text(
            text = localizedContext.getString(R.string.title_login),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(localizedContext.getString(R.string.label_email)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(localizedContext.getString(R.string.label_password)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Sign-in button
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(
                        localizedContext,
                        localizedContext.getString(R.string.toast_enter_credentials),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.login(
                        email,
                        password,
                        context = localizedContext,
                        onSuccess = { parentId, childUID ->
                            CoroutineScope(Dispatchers.IO).launch {
                                DataStoreHelper.saveParentInfo(localizedContext, parentId, childUID)
                            }

                            navController.navigate(Routes.PARENT_CHOICE) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.loading.value
        ) {
            Text(
                text = if (viewModel.loading.value)
                    localizedContext.getString(R.string.btn_logging_in)
                else
                    localizedContext.getString(R.string.btn_sign_in)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Error message
        viewModel.errorMessage.value?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Sign-up link
        TextButton(onClick = { navController.navigate(Routes.SIGNUP) }) {
            Text(localizedContext.getString(R.string.text_no_account))
        }

        // Loading indicator
        if (viewModel.loading.value) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}