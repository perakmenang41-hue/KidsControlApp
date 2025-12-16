package com.example.kidscontrolapp.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.kidscontrolapp.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import com.example.kidscontrolapp.viewmodel.LocaleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIssueScreen(
    navController: NavHostController,
    localeViewModel: LocaleViewModel,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val context = LocalContext.current

    // Get localized context
    val currentLocale by localeViewModel.locale.collectAsState()
    val localizedContext = remember(currentLocale) {
        localeViewModel.getLocalizedContext(context)
    }

    // UI states
    var subject by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var priority by remember { mutableStateOf("Medium") }
    var screenshotUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Screenshot picker
    val imagePicker = rememberLauncherForActivityResult(GetContent()) { uri ->
        screenshotUri = uri
    }

    val canSubmit = subject.text.isNotBlank() &&
            description.text.isNotBlank() &&
            !isSubmitting

    // -------------------- UI --------------------
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedContext.getString(R.string.title_report_issue)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.back),
                            contentDescription = localizedContext.getString(R.string.content_desc_back),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Subject
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text(localizedContext.getString(R.string.label_subject)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = subject.text.isBlank()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(localizedContext.getString(R.string.label_description)) },
                maxLines = 10,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                isError = description.text.isBlank()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Priority
            Text(
                text = localizedContext.getString(R.string.label_priority),
                style = MaterialTheme.typography.bodyMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Low", "Medium", "High").forEach { level ->
                    val labelRes = when (level) {
                        "Low" -> R.string.priority_low
                        "Medium" -> R.string.priority_medium
                        else -> R.string.priority_high
                    }

                    FilterChip(
                        selected = priority == level,
                        onClick = { priority = level },
                        label = { Text(localizedContext.getString(labelRes)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Screenshot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { imagePicker.launch("image/*") },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.clipfile),
                        contentDescription = localizedContext.getString(R.string.content_desc_add_screenshot),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(localizedContext.getString(R.string.btn_add_screenshot))
                }

                screenshotUri?.let { uri ->
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { screenshotUri = null },
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit
            Button(
                onClick = {
                    isSubmitting = true

                    val db = FirebaseFirestore.getInstance()
                    val data = hashMapOf(
                        "subject" to subject.text,
                        "description" to description.text,
                        "priority" to priority,
                        "timestamp" to Timestamp.now(),
                        "screenshot" to null
                    )

                    db.collection("bug_reports")
                        .add(data)
                        .addOnSuccessListener {
                            isSubmitting = false
                            navController.popBackStack()
                        }
                        .addOnFailureListener {
                            isSubmitting = false
                            val emailIntent = Intent(
                                Intent.ACTION_SENDTO,
                                Uri.parse("mailto:support@kidscontrolapp.com")
                            ).apply {
                                putExtra(Intent.EXTRA_SUBJECT, subject.text)
                                putExtra(Intent.EXTRA_TEXT, description.text)
                            }
                            context.startActivity(
                                Intent.createChooser(emailIntent, "Send email")
                            )
                        }
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(localizedContext.getString(R.string.btn_submit))
                }
            }
        }
    }
}
