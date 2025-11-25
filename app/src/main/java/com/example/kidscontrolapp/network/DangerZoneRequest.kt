package com.example.kidscontrolapp.network

data class DangerZoneRequest(
    val parentId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double
)
