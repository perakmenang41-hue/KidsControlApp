package com.example.kidscontrolapp.network

data class DangerZoneRequest(
    val parentId: String,
    val name: String,
    val lat: Double,      // FIXED
    val lon: Double,      // FIXED
    val radius: Double
)
