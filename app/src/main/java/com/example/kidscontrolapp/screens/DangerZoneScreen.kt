package com.example.kidscontrolapp.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.components.AddDangerZoneButton
import com.example.kidscontrolapp.utils.createCirclePoints
import com.example.kidscontrolapp.viewmodel.DangerZoneViewModel
import com.example.kidscontrolapp.viewmodel.LocaleViewModel
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
    parentId: String,
    childUid: String,
    currentLocation: GeoPoint? = null,
    localeViewModel: LocaleViewModel
) {
    val context = LocalContext.current
    val locale by localeViewModel.locale.collectAsState()

    // Create localized context for dynamic language
    val localizedContext = remember(locale) {
        val config = context.resources.configuration
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    val dangerZones by remember { derivedStateOf { viewModel.dangerZones } }

    // Parent-login guard
    if (parentId.isBlank()) {
        Toast.makeText(
            localizedContext,
            localizedContext.getString(R.string.msg_parent_not_logged_in),
            Toast.LENGTH_LONG
        ).show()
        navController.navigate("login") { popUpTo(0) }
        return
    }

    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var zoneName by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("50") }

    Box(modifier = Modifier.fillMaxSize()) {

        // MapView
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
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
            },
            update = { map ->
                map.overlays.clear()

                // Draw existing danger zones
                dangerZones.forEach { dz ->
                    val marker = Marker(map).apply {
                        position = GeoPoint(dz.lat, dz.lon)
                        icon = BitmapDrawable(
                            map.context.resources,
                            BitmapFactory.decodeResource(
                                map.context.resources,
                                R.drawable.danger
                            ).let { bmp ->
                                android.graphics.Bitmap.createScaledBitmap(bmp, 80, 80, false)
                            }
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
                            BitmapFactory.decodeResource(
                                map.context.resources,
                                R.drawable.danger
                            ).let { bmp ->
                                android.graphics.Bitmap.createScaledBitmap(bmp, 80, 80, false)
                            }
                        )
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    map.overlays.add(marker)

                    val polygon = Polygon().apply {
                        points = createCirclePoints(
                            loc.latitude,
                            loc.longitude,
                            radius.toDoubleOrNull() ?: 50.0
                        )
                        fillColor = android.graphics.Color.argb(50, 255, 0, 0)
                        strokeColor = android.graphics.Color.RED
                        strokeWidth = 3f
                    }
                    map.overlays.add(polygon)
                    map.controller.setCenter(loc)
                }

                // Tap-to-select handling
                val mapEvents = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?) =
                        p?.let {
                            selectedLocation = it
                            map.invalidate()
                            true
                        } ?: false

                    override fun longPressHelper(p: GeoPoint?) = false
                }
                map.overlays.add(MapEventsOverlay(mapEvents))
            },
            modifier = Modifier.fillMaxSize()
        )

        // Input fields
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = zoneName,
                onValueChange = { zoneName = it },
                label = { Text(localizedContext.getString(R.string.label_zone_name)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = radius,
                onValueChange = { radius = it },
                label = { Text(localizedContext.getString(R.string.label_radius)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Bottom-right FABs
        selectedLocation?.let { loc ->
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Delete button
                FloatingActionButton(
                    onClick = { navController.navigate("delete_zones/$parentId") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.delete),
                        contentDescription = localizedContext.getString(R.string.content_desc_manage_zones),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Add Danger Zone button
                AddDangerZoneButton(
                    parentId = parentId,
                    childUID = childUid,
                    childLat = loc.latitude,
                    childLng = loc.longitude,
                    zoneName = zoneName.ifBlank { localizedContext.getString(R.string.title_new_zone) },
                    radiusText = radius.ifBlank { "50" },
                    viewModel = viewModel,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        // Bottom-left Back FAB
        FloatingActionButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.back),
                contentDescription = localizedContext.getString(R.string.content_desc_back),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
