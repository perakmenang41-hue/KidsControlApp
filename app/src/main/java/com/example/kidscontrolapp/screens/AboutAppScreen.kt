package com.example.kidscontrolapp.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.kidscontrolapp.BuildConfig
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.viewmodel.LocaleViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(
    navController: NavHostController,
    localeViewModel: LocaleViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val locale by localeViewModel.locale.collectAsState()
    val appVersion = BuildConfig.VERSION_NAME

    // ✅ Handle SYSTEM / GESTURE BACK safely
    BackHandler {
        onBack()
    }

    val localizedContext = remember(locale) {
        val config = context.resources.configuration
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(localizedContext.getString(R.string.title_about_app))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = localizedContext.getString(R.string.back)
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = localizedContext.getString(R.string.app_logo_description),
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = localizedContext.getString(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = localizedContext.getString(R.string.version_format, appVersion),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = localizedContext.getString(R.string.about_description),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )

            val features = listOf(
                localizedContext.getString(R.string.feature_location),
                localizedContext.getString(R.string.feature_safe_zone),
                localizedContext.getString(R.string.feature_statistics),
                localizedContext.getString(R.string.feature_security),
                localizedContext.getString(R.string.feature_multilingual)
            )

            features.forEach {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text(it)
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_SENDTO,
                        Uri.parse("mailto:support@kidscontrolapp.com")
                    ).apply {
                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            localizedContext.getString(R.string.email_subject_support)
                        )
                    }
                    localizedContext.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Email, null)
                Spacer(Modifier.width(8.dp))
                Text(localizedContext.getString(R.string.title_contact_support))
            }

            OutlinedButton(
                onClick = {
                    localizedContext.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://kidscontrolapp.com"))
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Public, null)
                Spacer(Modifier.width(8.dp))
                Text(localizedContext.getString(R.string.title_visit_website))
            }

            TextButton(
                onClick = {
                    localizedContext.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://kidscontrolapp.com/privacy"))
                    )
                }
            ) {
                Text(localizedContext.getString(R.string.title_privacy_policy))
            }
        }
    }
}