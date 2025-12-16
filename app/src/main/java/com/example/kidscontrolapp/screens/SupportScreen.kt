package com.example.kidscontrolapp.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.kidscontrolapp.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource          // <-- added import
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

// Material Icons imports
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    navController: NavHostController,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val ctx = LocalContext.current
    var showBugDialog by remember { mutableStateOf(false) }

    // -----------------------------------------------------------------
    // FAQ data – now references string resources
    // -----------------------------------------------------------------
    val faqItems = listOf(
        stringResource(R.string.faq_q_reset_password) to
                stringResource(R.string.faq_a_reset_password),
        stringResource(R.string.faq_q_dark_mode) to
                stringResource(R.string.faq_a_dark_mode),
        stringResource(R.string.faq_q_contact_support) to
                stringResource(R.string.faq_a_contact_support)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_support)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back)
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            // -------------------------------------------------
            // Title
            // -------------------------------------------------
            Text(
                text = stringResource(R.string.title_faq),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // -------------------------------------------------
            // FAQ LIST
            // -------------------------------------------------
            faqItems.forEach { (question, answer) ->
                var expanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { expanded = !expanded },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = question,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess
                                else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (expanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = answer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // -------------------------------------------------
            // CONTACT BUTTONS
            // -------------------------------------------------
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Email Button
                OutlinedButton(
                    onClick = {
                        val email = "support@kidscontrolapp.com"
                        val intent = Intent(
                            Intent.ACTION_SENDTO,
                            Uri.parse("mailto:$email")
                        ).apply {
                            putExtra(Intent.EXTRA_SUBJECT, "KidsControlApp Support")
                        }
                        ctx.startActivity(intent)
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = stringResource(R.string.content_desc_email)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_email_us))
                }

                // Report Bug Button
                OutlinedButton(
                    onClick = { showBugDialog = true },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = stringResource(R.string.content_desc_bug_report)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_report_bug))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // -------------------------------------------------
            // Footer
            // -------------------------------------------------
            Text(
                text = stringResource(R.string.footer_version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // -------------------------------------------------
        // BUG REPORT DIALOG
        // -------------------------------------------------
        if (showBugDialog) {
            var subject by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showBugDialog = false },
                title = { Text(stringResource(R.string.dialog_bug_title)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text(stringResource(R.string.label_subject)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text(stringResource(R.string.label_description)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBugDialog = false }) {
                        Text(stringResource(R.string.btn_send))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBugDialog = false }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }
    }
}