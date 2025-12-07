package com.example.kidscontrolapp.network

// RiskResponse.kt
data class RiskResponse(
    val risk: String,
    val zoneName: String? = null,
    val zoneLat: Double,
    val zoneLon: Double
)
