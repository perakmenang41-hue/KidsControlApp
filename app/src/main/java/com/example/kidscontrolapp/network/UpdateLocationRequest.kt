package com.example.kidscontrolapp.network

data class UpdateLocationRequest(
    val childUID: String,
    val lat: Double,
    val lon: Double,
    val speed: Float,      // Add this
    val battery: Int,      // Add this
    val parentId: String
)
