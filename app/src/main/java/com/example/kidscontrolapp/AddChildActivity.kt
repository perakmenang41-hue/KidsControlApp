package com.example.kidscontrolapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kidscontrolapp.ui.theme.KidsControlAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.AndroidEntryPoint   // ← Hilt import

@AndroidEntryPoint                              // ← Hilt entry point
class AddChildActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KidsControlAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AddChildScreen { childName ->
                        val uid = auth.currentUser?.uid ?: return@AddChildScreen
                        database.child("children").child(uid).push().setValue(childName)
                        finish()
                    }
                }
            }
        }
    }
}

/* ----------------------------------------------------------------- */
/* The composable stays exactly the same – no changes required.      */
/* ----------------------------------------------------------------- */
@Composable
fun AddChildScreen(onAddChild: (String) -> Unit) {
    var childName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = childName,
            onValueChange = { childName = it },
            label = { Text("Child Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { if (childName.isNotBlank()) onAddChild(childName) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Child")
        }
    }
}