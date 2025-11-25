package com.example.kidscontrolapp.components

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.kidscontrolapp.R
import com.yalantis.ucrop.UCrop
import java.io.File

@Composable
fun ProfileImagePicker(onImageSelected: (Uri) -> Unit) {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher for picking image
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Destination for cropped image
            val destinationUri = Uri.fromFile(
                File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
            )
            val uCrop = UCrop.of(uri, destinationUri)
            uCrop.withAspectRatio(1f, 1f) // square crop
            uCrop.withMaxResultSize(512, 512)
            uCrop.start(context as Activity)
        }
    }

    // UI
    Box(modifier = Modifier.clickable { pickImageLauncher.launch("image/*") }) {
        if (selectedImageUri != null) {
            AsyncImage(
                model = selectedImageUri,
                contentDescription = "Profile Image",
                modifier = Modifier.size(100.dp)
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.user), // ✅ correct import
                contentDescription = "Default Profile",
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.LightGray.copy(alpha = 0.4f), CircleShape)
            )
        }
    }
}
