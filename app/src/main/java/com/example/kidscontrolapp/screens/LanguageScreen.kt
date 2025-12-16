package com.example.kidscontrolapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.viewmodel.LocaleViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    navController: NavHostController,
    localeViewModel: LocaleViewModel,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val currentLocale by localeViewModel.locale.collectAsState()
    val currentLocaleTag = currentLocale?.toLanguageTag() ?: "system"

    var pendingCode by remember { mutableStateOf(currentLocaleTag) }

    val languages = listOf(
        "system" to stringResource(R.string.language_system),
        "en" to stringResource(R.string.language_english),
        "es" to stringResource(R.string.language_spanish),
        "fr" to stringResource(R.string.language_french),
        "ms" to stringResource(R.string.language_malay),
        "zh-CN" to stringResource(R.string.language_chinese_simplified),
        "zh-TW" to stringResource(R.string.language_chinese_traditional)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_language)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.back),
                            contentDescription = stringResource(R.string.content_desc_back)
                        )
                    }
                },
                actions = {
                    if (pendingCode != currentLocaleTag) {
                        IconButton(
                            onClick = {
                                // Apply the selected language
                                localeViewModel.selectLanguage(pendingCode)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(R.string.content_desc_save_language)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            languages.forEach { (code, label) ->
                val isSelected = pendingCode == code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pendingCode = code }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = { pendingCode = code })
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = label, style = MaterialTheme.typography.bodyLarge)
                }
                Divider()
            }
        }
    }
}
