package com.example.kidscontrolapp.network

data class UpdateLocationRequest(
    val parentId: String,
    val childUID: String,
    val lat: Double,
    val lon: Double
)
