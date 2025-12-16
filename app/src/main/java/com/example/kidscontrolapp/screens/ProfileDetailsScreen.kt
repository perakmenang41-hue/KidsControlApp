package com.example.kidscontrolapp.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.kidscontrolapp.components.SectionScaffold
import com.example.kidscontrolapp.data.DataStoreHelper
import com.example.kidscontrolapp.data.ParentInfo
import com.example.kidscontrolapp.viewmodel.LocaleViewModel
import com.example.kidscontrolapp.viewmodel.ProfileDetailsViewModel
import com.example.kidscontrolapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailsScreen(
    navController: NavController,
    localeViewModel: LocaleViewModel,
    viewModel: ProfileDetailsViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentLocale by localeViewModel.locale.collectAsState()
    val localizedContext = remember(currentLocale) {
        val config = context.resources.configuration
        config.setLocale(currentLocale)
        context.createConfigurationContext(config)
    }

    // -------------------------------------------------
    // Load parentId
    // -------------------------------------------------
    var parentId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        DataStoreHelper.getParentId(context).collect { pid ->
            if (!pid.isNullOrBlank()) {
                parentId = pid
                viewModel.load(pid)
            }
        }
    }

    // -------------------------------------------------
    // Observe Firestore
    // -------------------------------------------------
    val parentInfo by viewModel.parentInfo.collectAsState()

    // -------------------------------------------------
    // Local editable fields
    // -------------------------------------------------
    var name by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    LaunchedEffect(parentInfo) {
        parentInfo?.let {
            name = it.name
            country = it.country
            email = it.email
            phone = it.phone
        }
    }

    // -------------------------------------------------
    // Save result handling
    // -------------------------------------------------
    val saveResult by viewModel.saveResult.collectAsState()
    LaunchedEffect(saveResult) {
        saveResult?.let { success ->
            val toastMsg = if (success) {
                localizedContext.getString(R.string.toast_profile_saved)
            } else {
                localizedContext.getString(R.string.toast_profile_save_failed)
            }
            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
            if (success) navController.popBackStack()
            viewModel.clearResult()
        }
    }

    // -------------------------------------------------
    // UI
    // -------------------------------------------------
    SectionScaffold(
        title = localizedContext.getString(R.string.title_profile_details),
        navController = navController
    ) {
        val info = parentInfo
        if (info == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(localizedContext.getString(R.string.label_legal_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                // Country
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text(localizedContext.getString(R.string.label_country)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                // User ID (read-only)
                OutlinedTextField(
                    value = info.parentId,
                    onValueChange = {},
                    label = { Text(localizedContext.getString(R.string.label_user_id)) },
                    readOnly = true,
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(localizedContext.getString(R.string.label_email)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(localizedContext.getString(R.string.label_phone_number)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                // Save button
                Button(
                    onClick = {
                        val edited = ParentInfo(
                            name = name,
                            country = country,
                            email = email,
                            phone = phone,
                            parentId = info.parentId
                        )
                        parentId?.let { viewModel.saveChanges(it, edited) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(localizedContext.getString(R.string.btn_save_change))
                }
            }
        }
    }
}
