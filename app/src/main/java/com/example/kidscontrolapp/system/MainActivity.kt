package com.example.kidscontrolapp.system

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.google.firebase.firestore.FirebaseFirestore
import com.example.kidscontrolapp.navigation.MainNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable Firestore logging
        FirebaseFirestore.setLoggingEnabled(true)

        setContent {
            MaterialTheme {
                Surface {
                    // ✅ Use MainNavigation for all app navigation
                    MainNavigation()
                }
            }
        }
    }
}
