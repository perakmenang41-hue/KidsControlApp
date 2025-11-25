// MapUtils.kt
package com.example.kidscontrolapp.utils

import org.osmdroid.util.GeoPoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun createCirclePoints(
    lat: Double,
    lon: Double,
    radiusMeters: Double,
    pointsCount: Int = 36
): List<GeoPoint> {
    val result = mutableListOf<GeoPoint>()
    val radiusDegrees = radiusMeters / 111320.0
    for (i in 0 until pointsCount) {
        val angle = 2 * PI * i / pointsCount
        val dx = radiusDegrees * cos(angle)
        val dy = radiusDegrees * sin(angle)
        result.add(GeoPoint(lat + dy, lon + dx))
    }
    return result
}
