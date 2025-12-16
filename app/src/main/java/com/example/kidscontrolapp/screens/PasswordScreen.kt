package com.example.kidscontrolapp.screens

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
import androidx.navigation.NavController
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.components.SectionScaffold
import com.example.kidscontrolapp.viewmodel.LocaleViewModel
import com.example.kidscontrolapp.viewmodel.PasswordViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kidscontrolapp.data.DataStoreHelper
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordScreen(
    navController: NavController,
    localeViewModel: LocaleViewModel,
    passwordViewModel: PasswordViewModel = viewModel()
) {
    val context = LocalContext.current

    // Observe current locale for instant text updates
    val locale by localeViewModel.locale.collectAsState()
    val localizedContext = remember(locale) {
        val config = context.resources.configuration
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    // Load stored parentId
    var parentId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        parentId = DataStoreHelper.getParentId(context).first()
    }

    var currentPwd by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }

    val loading by passwordViewModel.loading.collectAsState()
    val errorMsg by passwordViewModel.errorMessage.collectAsState()
    val success by passwordViewModel.success.collectAsState()

    // React to success / error
    LaunchedEffect(success, errorMsg) {
        when {
            success == true -> {
                Toast.makeText(
                    context,
                    localizedContext.getString(R.string.toast_password_updated),
                    Toast.LENGTH_SHORT
                ).show()
                passwordViewModel.clearStatus()
                navController.popBackStack()
            }
            errorMsg != null -> {
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                passwordViewModel.clearStatus()
            }
        }
    }

    SectionScaffold(
        title = localizedContext.getString(R.string.title_change_password),
        navController = navController
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current password
            OutlinedTextField(
                value = currentPwd,
                onValueChange = { currentPwd = it },
                label = { Text(localizedContext.getString(R.string.label_current_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // New password
            OutlinedTextField(
                value = newPwd,
                onValueChange = { newPwd = it },
                label = { Text(localizedContext.getString(R.string.label_new_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Confirm new password
            OutlinedTextField(
                value = confirmPwd,
                onValueChange = { confirmPwd = it },
                label = { Text(localizedContext.getString(R.string.label_confirm_new_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Save button
            Button(
                onClick = {
                    passwordViewModel.changePassword(
                        currentPassword = currentPwd,
                        newPassword = newPwd,
                        confirmPassword = confirmPwd
                    )
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(localizedContext.getString(R.string.btn_save))
                }
            }
        }
    }
}
