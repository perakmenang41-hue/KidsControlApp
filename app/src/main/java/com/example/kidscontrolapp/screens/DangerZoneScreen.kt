package com.example.kidscontrolapp.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.model.DangerZone
import com.example.kidscontrolapp.utils.createCirclePoints
import com.example.kidscontrolapp.viewmodel.DangerZoneViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DangerZoneScreen(
    navController: NavHostController,
    viewModel: DangerZoneViewModel,
    currentLocation: GeoPoint? = null
) {
    val context = LocalContext.current
    val dangerZones by remember { derivedStateOf { viewModel.dangerZones } }

    val sharedPrefs = context.getSharedPreferences("parent_prefs", Context.MODE_PRIVATE)
    val parentUid = sharedPrefs.getString("parentId", "") ?: ""

    LaunchedEffect(Unit) {
        viewModel.fetchZones(parentUid)
    }

    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var zoneName by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("50") } // user input as String
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<DangerZone?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        // ----------------------------
        // MapView
        // ----------------------------
        AndroidView(
            factory = { ctx ->
                val mapView = MapView(ctx).apply {
                    Configuration.getInstance().load(
                        ctx,
                        androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx)
                    )
                    setTileSource(TileSourceFactory.MAPNIK)
                    setBuiltInZoomControls(true)
                    setMultiTouchControls(true)
                    controller.setZoom(18.0)
                    controller.setCenter(currentLocation ?: GeoPoint(3.1390, 101.6869))
                }

                val gestureDetector = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        val tappedMarker = mapView.overlays
                            .filterIsInstance<Marker>()
                            .firstOrNull { marker ->
                                val screenPoint = mapView.projection.toPixels(marker.position, null)
                                val distance = Math.hypot(
                                    (e.x - screenPoint.x).toDouble(),
                                    (e.y - screenPoint.y).toDouble()
                                )
                                distance < 80
                            }
                        tappedMarker?.let { marker ->
                            val dz = dangerZones.find { it.lat == marker.position.latitude && it.lon == marker.position.longitude }
                            dz?.let { showDeleteDialog = it }
                        }
                        return true
                    }
                })

                mapView.setOnTouchListener { _, event ->
                    gestureDetector.onTouchEvent(event)
                    false
                }

                mapView
            },
            update = { map ->
                map.overlays.clear()

                // Draw existing danger zones
                dangerZones.forEach { dz ->
                    val marker = Marker(map).apply {
                        position = GeoPoint(dz.lat, dz.lon)
                        icon = BitmapDrawable(
                            map.context.resources,
                            BitmapFactory.decodeResource(map.context.resources, R.drawable.danger)
                                .let { bmp -> android.graphics.Bitmap.createScaledBitmap(bmp, 80, 80, false) }
                        )
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    map.overlays.add(marker)

                    val polygon = Polygon().apply {
                        points = createCirclePoints(dz.lat, dz.lon, dz.radius)
                        fillColor = android.graphics.Color.argb(50, 255, 0, 0)
                        strokeColor = android.graphics.Color.RED
                        strokeWidth = 3f
                    }
                    map.overlays.add(polygon)
                }

                // Draw selected location
                selectedLocation?.let { loc ->
                    val marker = Marker(map).apply {
                        position = loc
                        icon = BitmapDrawable(
                            map.context.resources,
                            BitmapFactory.decodeResource(map.context.resources, R.drawable.danger)
                                .let { bmp -> android.graphics.Bitmap.createScaledBitmap(bmp, 80, 80, false) }
                        )
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    map.overlays.add(marker)

                    val polygon = Polygon().apply {
                        points = createCirclePoints(loc.latitude, loc.longitude, radius.toDoubleOrNull() ?: 50.0)
                        fillColor = android.graphics.Color.argb(50, 255, 0, 0)
                        strokeColor = android.graphics.Color.RED
                        strokeWidth = 3f
                    }
                    map.overlays.add(polygon)
                    map.controller.setCenter(loc)
                }

                val mapEvents = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        selectedLocation = p
                        return true
                    }
                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                }
                map.overlays.add(MapEventsOverlay(mapEvents))

                map.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // ----------------------------
        // Input Fields
        // ----------------------------
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = zoneName,
                onValueChange = { zoneName = it },
                label = { Text("Zone Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = radius,
                onValueChange = { radius = it },
                label = { Text("Radius (m)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ----------------------------
        // Floating Add Button
        // ----------------------------
        FloatingActionButton(
            onClick = { if (selectedLocation != null && zoneName.isNotBlank() && radius.isNotBlank()) showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.add),
                contentDescription = "Add Zone",
                modifier = Modifier.size(24.dp)
            )
        }

        // ----------------------------
        // Floating Back Button
        // ----------------------------
        FloatingActionButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.secondary
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.back),
                contentDescription = "Back",
                modifier = Modifier.size(24.dp)
            )
        }

        // ----------------------------
        // Add Confirmation Dialog
        // ----------------------------
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Danger Zone?") },
                text = { Text("Are you sure you want to add this danger zone?") },
                confirmButton = {
                    TextButton(onClick = {
                        val radiusValue = radius.toDoubleOrNull() ?: 50.0
                        selectedLocation?.let { loc ->
                            viewModel.addZone(
                                DangerZone(
                                    id = System.currentTimeMillis().toString(), // String ID
                                    name = zoneName,
                                    lat = loc.latitude,
                                    lon = loc.longitude,
                                    radius = radiusValue
                                )
                            )
                        }
                        zoneName = ""
                        radius = "50"
                        selectedLocation = null
                        showAddDialog = false
                    }) { Text("Yes") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("No") }
                }
            )
        }

        // ----------------------------
        // Delete / Info Dialog
        // ----------------------------
        showDeleteDialog?.let { dz ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text(dz.name) },
                text = { Text("Radius: ${dz.radius} meters") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.removeZone(dz)
                        showDeleteDialog = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
                }
            )
        }
    }
}
