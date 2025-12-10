package com.example.kidscontrolapp.system

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.kidscontrolapp.network.ApiService
import com.example.kidscontrolapp.tracking.LocationTracker
import com.example.kidscontrolapp.navigation.MainNavigation
import android.Manifest
import android.content.pm.PackageManager

class MainActivity : ComponentActivity() {

    private lateinit var tracker: LocationTracker
    private val TAG = "MainActivity"
    private val LOCATION_PERMISSION_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable Firestore logging
        com.google.firebase.firestore.FirebaseFirestore.setLoggingEnabled(true)


        setContent {
            MaterialTheme {
                Surface {
                    MainNavigation()
                }
            }
        }
    }
}

