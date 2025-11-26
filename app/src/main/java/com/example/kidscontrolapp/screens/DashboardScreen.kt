package com.example.kidscontrolapp.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Location
import com.example.kidscontrolapp.service.DangerZoneService
import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.components.TopBar
import com.example.kidscontrolapp.navigation.Routes
import com.example.kidscontrolapp.utils.createCirclePoints
import com.example.kidscontrolapp.utils.sendDangerNotification
import com.example.kidscontrolapp.viewmodel.DangerZoneViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Polygon
import java.io.Serializable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// -----------------------------
// Data classes
// -----------------------------
data class LocationLog(
    val latitude: Double,
    val longitude: Double,
    val battery: Double,
    val lastUpdated: Long
)

data class StorePOI(val name: String, val lat: Double, val lon: Double)

// Make DangerZone Serializable
data class DangerZone(
    val id: Int,
    val name: String,
    val lat: Double,
    val lon: Double,
    val radius: Double
) : Serializable

// -----------------------------
// Dashboard with Drawer
// -----------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardWithDrawer(
    childUID: String,
    navController: NavHostController,
    dangerZoneViewModel: DangerZoneViewModel
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> profileImageUri = uri }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.back),
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (profileImageUri != null) {
                            AsyncImage(
                                model = profileImageUri,
                                contentDescription = "Profile Image",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .clickable { pickImageLauncher.launch("image/*") }
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.user),
                                contentDescription = "Default Profile",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .clickable { pickImageLauncher.launch("image/*") }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { pickImageLauncher.launch("image/*") }) {
                            Text("Edit Profile")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { navController.navigate(Routes.DANGER_ZONE) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Danger Zone Settings")
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DashboardScreen(
                navController = navController,
                childUID = childUID,
                profileImageUri = profileImageUri?.toString(),
                onProfileClick = { scope.launch { drawerState.open() } },
                onImagePick = { pickImageLauncher.launch("image/*") },
                dangerZoneViewModel = dangerZoneViewModel
            )

            if (drawerState.isOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { scope.launch { drawerState.close() } }
                )
            }
        }
    }
}

// -----------------------------
// Dashboard Screen
// -----------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    childUID: String,
    profileImageUri: String?,
    onProfileClick: () -> Unit,
    onImagePick: () -> Unit = {},
    dangerZoneViewModel: DangerZoneViewModel
) {
    var tracking by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopBar(
                title = "Dashboard",
                navController = navController,
                onProfileClick = onProfileClick,
                onImagePick = onImagePick,
                profileImage = profileImageUri,
                showBackButton = false
            )
        },
        bottomBar = { BottomNavigationBar() }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            TrackingContentSafe(
                childUID = childUID,
                trackingStarted = tracking,
                dangerZoneViewModel = dangerZoneViewModel
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        tracking = true
                        val intent = Intent(context, DangerZoneService::class.java).apply {
                            putExtra(DangerZoneService.CHILD_UID, childUID)
                            putExtra(
                                DangerZoneService.ZONES,
                                ArrayList(dangerZoneViewModel.dangerZones) as Serializable
                            )
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Start Tracking"
                    )
                }

                FloatingActionButton(
                    onClick = {
                        tracking = false
                        val intent = Intent(context, DangerZoneService::class.java)
                        context.stopService(intent)
                    },
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(
                        painter = painterResource(R.drawable.stop),
                        contentDescription = "Stop Tracking",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// -----------------------------
// Bottom Navigation
// -----------------------------
@Composable
fun BottomNavigationBar() {
    NavigationBar {
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.Email, contentDescription = "Inbox") })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.LocationOn, contentDescription = "Location") })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.Person, contentDescription = "Profile") })
    }
}

// -----------------------------
// Tracking Content
// -----------------------------
@Composable
fun TrackingContentSafe(
    childUID: String,
    trackingStarted: Boolean,
    dangerZoneViewModel: DangerZoneViewModel
) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    val locationHistory = remember { mutableStateListOf<LocationLog>() }
    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }

    val dangerZones by remember { derivedStateOf { dangerZoneViewModel.dangerZones.toList() } }
    val alertedZones = remember { mutableStateListOf<String>() }

    // -----------------------------
    // 1. Listen to child location updates
    // -----------------------------
    LaunchedEffect(trackingStarted) {
        if (trackingStarted && childUID.isNotBlank()) {
            listenerRegistration?.remove()
            listenerRegistration = firestore.collection("child_locations")
                .document(childUID)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val lat = snapshot.getDouble("latitude")
                        val lon = snapshot.getDouble("longitude")
                        val battery = snapshot.getDouble("battery") ?: 100.0
                        val lastUpdated = snapshot.getTimestamp("lastUpdated")?.toDate()?.time
                            ?: System.currentTimeMillis()

                        if (lat != null && lon != null) {
                            currentLocation = Location("").apply {
                                latitude = lat
                                longitude = lon
                            }

                            // Update history
                            locationHistory.add(0, LocationLog(lat, lon, battery, lastUpdated))
                            if (locationHistory.size > 5) locationHistory.removeAt(locationHistory.lastIndex)
                        }
                    }
                }
        } else {
            listenerRegistration?.remove()
            listenerRegistration = null
            currentLocation = null
            locationHistory.clear()
            alertedZones.clear()
        }
    }

    // -----------------------------
    // 2. Check danger zones whenever current location or zones change
    // -----------------------------
    LaunchedEffect(currentLocation, dangerZones) {
        currentLocation?.let { loc ->
            dangerZones.forEach { zone ->
                val distance = FloatArray(1)
                Location.distanceBetween(
                    loc.latitude,
                    loc.longitude,
                    zone.lat,
                    zone.lon,
                    distance
                )
                if (distance[0] <= zone.radius && !alertedZones.contains(zone.id)) {
                    sendDangerNotification(context, zone.name)
                    alertedZones.add(zone.id)
                } else if (distance[0] > zone.radius) {
                    alertedZones.remove(zone.id)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (currentLocation != null) {
            DashboardMapView(
                current = currentLocation,
                history = locationHistory.toList(),
                storeMarkers = emptyList(),
                childUID = childUID,
                dangerZoneViewModel = dangerZoneViewModel
            )
        } else {
            Text(
                "Press Start to show child location on the map",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}



// -----------------------------
// MapView Composable
// -----------------------------
@Composable
fun DashboardMapView(
    current: Location?,
    history: List<LocationLog>,
    storeMarkers: List<StorePOI> = emptyList(),
    childUID: String,
    dangerZoneViewModel: DangerZoneViewModel
) {
    val context = LocalContext.current
    val dangerZones by remember { derivedStateOf { dangerZoneViewModel.dangerZones.toList() } }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                try {
                    Configuration.getInstance().load(
                        ctx,
                        androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx)
                    )
                } catch (_: Exception) {}
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isClickable = true
                setBuiltInZoomControls(true)
                controller.setZoom(18.0)
                controller.setCenter(current?.let { GeoPoint(it.latitude, it.longitude) } ?: GeoPoint(3.1390, 101.6869))
            }
        },
        update = { map ->
            try {
                map.overlays.clear()

                val points = history.map { GeoPoint(it.latitude, it.longitude) }
                if (points.isNotEmpty()) map.overlays.add(Polyline().apply { setPoints(points) })

                history.forEach {
                    map.overlays.add(Marker(map).apply {
                        position = GeoPoint(it.latitude, it.longitude)
                        title = "Last updated: ${it.lastUpdated}"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    })
                }

                current?.let {
                    map.overlays.add(Marker(map).apply {
                        position = GeoPoint(it.latitude, it.longitude)
                        title = "Current location"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    })
                    map.controller.setCenter(GeoPoint(it.latitude, it.longitude))
                }

                storeMarkers.forEach { store ->
                    map.overlays.add(Marker(map).apply {
                        position = GeoPoint(store.lat, store.lon)
                        title = store.name
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    })
                }

                dangerZones.forEach { zone ->
                    map.overlays.add(Polygon().apply {
                        setPoints(createCirclePoints(zone.lat, zone.lon, zone.radius))
                        fillColor = Color.Red.copy(alpha = 0.2f).toArgb()
                        strokeColor = Color.Red.toArgb()
                        strokeWidth = 2f
                    })

                    map.overlays.add(Marker(map).apply {
                        position = GeoPoint(zone.lat, zone.lon)
                        title = "${zone.name} (${zone.radius}m)"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = BitmapDrawable(
                            context.resources,
                            BitmapFactory.decodeResource(context.resources, R.drawable.danger).let { bmp ->
                                android.graphics.Bitmap.createScaledBitmap(bmp, 80, 80, false)
                            }
                        )
                        setOnMarkerClickListener { _, _ ->
                            Toast.makeText(context, "Zone: ${zone.name}\nRadius: ${zone.radius}m", Toast.LENGTH_LONG).show()
                            true
                        }
                    })
                }

                map.invalidate()
            } catch (_: Exception) {}
        }
    )
}

// -----------------------------
// Circle helper
// -----------------------------
fun createCirclePoints(lat: Double, lon: Double, radiusMeters: Double, pointsCount: Int = 36): List<GeoPoint> {
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
