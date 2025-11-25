package com.example.kidscontrolapp.screens

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.views.MapView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import com.example.kidscontrolapp.R

@SuppressLint("MissingPermission")
@Composable
fun TrackerMapScreen(
    context: Context,
    childLat: Double,
    childLng: Double
) {
    var marker: Marker? by remember { mutableStateOf(null) }
    var mapView: MapView? by remember { mutableStateOf(null) }

    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osm", Context.MODE_PRIVATE))

            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                controller.setZoom(18.0)

                val startPoint = GeoPoint(childLat, childLng)
                controller.setCenter(startPoint)

                // Drone marker
                val drone = Marker(this)
                drone.icon = ctx.getDrawable(R.drawable.drone)
                drone.position = startPoint
                drone.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                overlays.add(drone)
                marker = drone
                mapView = this
            }
        },
        update = {
            mapView?.let { map ->
                marker?.let { drone ->
                    val newPoint = GeoPoint(childLat, childLng)
                    drone.position = newPoint
                    drone.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                    // smooth camera follow
                    map.controller.animateTo(newPoint)
                }
            }
        }
    )
}
