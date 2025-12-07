package com.example.kidscontrolapp.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.example.kidscontrolapp.R

class ChildMapActivity : ComponentActivity() {

    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext))

        map = MapView(this)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        setContentView(map)

        val startPoint = GeoPoint(-6.200000, 106.816666) // Jakarta example
        map.controller.setZoom(15.0)
        map.controller.setCenter(startPoint)

        val marker = Marker(map)
        marker.position = startPoint
        marker.icon = getDrawable(R.drawable.drone)
        marker.title = "Drone Location"
        map.overlays.add(marker)
    }
}
