package com.example.kidscontrolapp.tracking

import android.content.Context

/**
 * Minimal contract that the tracking manager needs.
 * Both the UI ViewModel and the Service‑side helper implement this.
 */
interface LocationHandler {
    /** Called by the service when a new GPS point arrives. */
    fun updateLocation(lat: Double, lon: Double, speed: Float, battery: Int)

    /** Sends a single location update to the backend. */
    fun sendChildLocationUpdate(
        context: Context,
        parentId: String,
        childUID: String,
        lat: Double,
        lon: Double,
        battery: Int,
        speed: Float
    )

    /** Called periodically by `TrackingManager` (every 5 s). */
    fun sendLocationToBackend(parentId: String, childUID: String)
}