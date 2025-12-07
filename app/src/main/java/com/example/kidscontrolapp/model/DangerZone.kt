package com.example.kidscontrolapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DangerZone(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val radius: Double
) : Parcelable
