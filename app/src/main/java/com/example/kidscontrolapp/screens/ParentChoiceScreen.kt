package com.example.kidscontrolapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.navigation.Routes
import com.example.kidscontrolapp.viewmodel.LocaleViewModel

@Composable
fun ParentChoiceScreen(
    navController: NavHostController,
    localeViewModel: LocaleViewModel
) {
    val context = LocalContext.current
    val locale by localeViewModel.locale.collectAsState()

    val localizedContext = remember(locale) {
        val config = context.resources.configuration
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = localizedContext.getString(R.string.title_welcome_parent),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = localizedContext.getString(R.string.subtitle_choose_continue),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { navController.navigate(Routes.ENTER_UID) },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = localizedContext.getString(R.string.btn_add_new_child),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = { navController.navigate(Routes.ENTER_UID) },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = localizedContext.getString(R.string.btn_use_existing_child),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
