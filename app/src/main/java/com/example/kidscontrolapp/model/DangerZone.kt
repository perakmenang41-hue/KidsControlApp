package com.example.kidscontrolapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DangerZone(
    val id: String = "",  // default empty string for new zones
    val name: String,
    val lat: Double,
    val lon: Double,
    val radius: Double
) : Parcelable
