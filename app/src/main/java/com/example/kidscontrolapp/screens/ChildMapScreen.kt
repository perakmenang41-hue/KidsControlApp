package com.example.kidscontrolapp.screens

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun ChildMapScreen(navController: NavHostController) {
    val context: Context = LocalContext.current

    // Initialize OSMDroid configuration
    Configuration.getInstance().load(
        context,
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
    )

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)

                controller.setZoom(15.0)
                val startPoint = GeoPoint(-6.200000, 106.816666)
                controller.setCenter(startPoint)

                val marker = Marker(this)
                marker.position = startPoint
                marker.title = "Drone Location"
                marker.icon = ctx.getDrawable(com.example.kidscontrolapp.R.drawable.drone)
                overlays.add(marker)
            }
        }
    )
}
