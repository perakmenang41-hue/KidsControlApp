package com.example.kidscontrolapp.system

import com.google.firebase.database.FirebaseDatabase

class FirebaseTrackingManager(private val database: FirebaseDatabase) {

    fun updateLocation(
        childId: String,
        lat: Double,
        lon: Double,
        accuracy: Float,
        address: String
    ) {
        val locationRef = database.getReference("children/$childId/location")
        val data = mapOf(
            "latitude" to lat,
            "longitude" to lon,
            "accuracy" to accuracy,
            "address" to address,
            "timestamp" to System.currentTimeMillis()
        )
        locationRef.setValue(data)
    }
}
