package com.example.kidscontrolapp.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import com.example.kidscontrolapp.components.DangerZoneAlertBox
import com.example.kidscontrolapp.viewmodel.DangerZoneAlertViewModel
import com.example.kidscontrolapp.data.DataStoreHelper
import kotlinx.coroutines.tasks.await
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.kidscontrolapp.tracking.LocationTracker
import com.example.kidscontrolapp.network.ApiService
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.kidscontrolapp.R
import com.example.kidscontrolapp.components.TopBar
import com.example.kidscontrolapp.service.DangerZoneService
import com.example.kidscontrolapp.utils.FirestoreProvider
import com.example.kidscontrolapp.viewmodel.ChildLocationViewModel
import com.example.kidscontrolapp.viewmodel.DangerZoneViewModel
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// -----------------------------
// Data classes
// -----------------------------
data class StorePOI(val name: String, val lat: Double, val lon: Double)

data class LocationLog(
    val latitude: Double,
    val longitude: Double,
    val battery: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Parcelize
data class DangerZone(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val radius: Double
) : Parcelable

// ==============================
// Permission requester
// ==============================
@Composable
fun LocationPermissionRequester(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) onPermissionsGranted()
        else Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
    }
    LaunchedEffect(Unit) {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
            )
        )
    }
}

// ==========================
// TrackingContentSafe
// (UI: DOES NOT show map until trackingStarted==true)
// ==========================
@Composable
fun TrackingContentSafe(
    parentId: String,
    childUID: String,
    trackingStarted: Boolean = false,
    dangerZoneViewModel: DangerZoneViewModel = viewModel(),
    childLocationViewModel: ChildLocationViewModel = viewModel()
) {
    val context = LocalContext.current
    val firestore = FirestoreProvider.getFirestore()

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    val locationHistory = remember { mutableStateListOf<LocationLog>() }
    var batteryLevel by remember { mutableStateOf(0.0) }
    var status by remember { mutableStateOf("offline") }
    var listener: ListenerRegistration? by remember { mutableStateOf(null) }

    val dangerZones by remember { derivedStateOf { dangerZoneViewModel.dangerZones.toList() } }

    // Store last valid child location so map doesn't jump
    val lastValidCameraPosition = remember { mutableStateOf<Location?>(null) }

    // Helper to detect invalid / emulator default location
    fun Location.isValid(): Boolean {
        return !(latitude == 0.0 && longitude == 0.0) &&
                !(latitude == 37.4219983 && longitude == -122.084)
    }

    // Attach Firestore listener only when tracking started
    LaunchedEffect(trackingStarted, childUID) {
        listener?.remove()
        listener = null

        if (trackingStarted) {
            try {
                listener = firestore.collection("child_position")
                    .document(childUID)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("TrackingContentSafe", "Listener error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            val lat = snapshot.getDouble("lat")
                            val lon = snapshot.getDouble("lon")
                            val batt = snapshot.getDouble("battery") ?: 0.0
                            val childStatus = snapshot.getString("status") ?: "offline"

                            if (lat != null && lon != null) {
                                val loc = Location("").apply { latitude = lat; longitude = lon }
                                if (loc.isValid()) {
                                    currentLocation = loc
                                    lastValidCameraPosition.value = loc
                                    locationHistory.add(LocationLog(lat, lon, batt))
                                } else {
                                    Log.w("TrackingContentSafe", "Ignored invalid location: $lat, $lon")
                                }
                            }

                            batteryLevel = batt
                            status = childStatus
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error listening location: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Stop tracking: clear UI
            currentLocation = null
            locationHistory.clear()
            batteryLevel = 0.0
            status = "offline"
            lastValidCameraPosition.value = null
        }
    }

    // Clean up listener
    DisposableEffect(Unit) {
        onDispose {
            listener?.remove()
            listener = null
        }
    }

    // ------------------------
    // UI
    // ------------------------
    Box(modifier = Modifier.fillMaxSize()) {
        if (trackingStarted && lastValidCameraPosition.value != null) {
            DashboardMapView(
                current = lastValidCameraPosition.value!!,
                history = locationHistory.toList(),
                storeMarkers = emptyList(),
                childUID = childUID,
                dangerZoneViewModel = dangerZoneViewModel
            )
        } else {
            // Placeholder while waiting for first valid child location
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.02f)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.LocationOff, contentDescription = null, modifier = Modifier.size(48.dp))
                Text("Tracking is off or waiting for child location.", modifier = Modifier.padding(8.dp))
            }
        }

        // Status overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .padding(8.dp)
        ) {
            Text("Battery: $batteryLevel%", style = MaterialTheme.typography.bodySmall)
            Text("Status: $status", style = MaterialTheme.typography.bodySmall)
        }
    }
}




@Composable
fun DashboardAutoTrackingScreen(
    navController: NavHostController,
    dangerZoneViewModel: DangerZoneViewModel = viewModel(),
    childLocationViewModel: ChildLocationViewModel = viewModel()
) {
    val context = LocalContext.current
    var parentId by remember { mutableStateOf<String?>(null) }
    var childUID by remember { mutableStateOf<String?>(null) }
    var trackingStarted by remember { mutableStateOf(false) }

    // Load DataStore values
    LaunchedEffect(Unit) {
        DataStoreHelper.getParentId(context).collect { parentId = it }
    }
    LaunchedEffect(Unit) {
        DataStoreHelper.getChildUID(context).collect { childUID = it }
    }

    // default: start tracking automatically only if you want
    LaunchedEffect(parentId, childUID) {
        if (!parentId.isNullOrBlank() && !childUID.isNullOrBlank()) {
            // keep false by default; you can set to true to auto-start
            trackingStarted = false
        }
    }

    if (parentId != null && childUID != null) {
        TrackingContentSafe(
            parentId = parentId!!,
            childUID = childUID!!,
            trackingStarted = trackingStarted,
            dangerZoneViewModel = dangerZoneViewModel,
            childLocationViewModel = childLocationViewModel
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading user info…")
        }
    }
}

// ==========================
// TrackingContentWrapper
// ==========================
@Composable
fun TrackingContentWrapper(
    parentId: String,   // <-- add this
    childUID: String,
    dangerZoneViewModel: DangerZoneViewModel,
    childLocationViewModel: ChildLocationViewModel
) {
    var permissionsGranted by remember { mutableStateOf(false) }
    var trackingStarted by remember { mutableStateOf(false) }

    if (!permissionsGranted) {
        LocationPermissionRequester {
            permissionsGranted = true
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            TrackingContentSafe(
                parentId = parentId,
                childUID = childUID,
                trackingStarted = trackingStarted,
                dangerZoneViewModel = dangerZoneViewModel,
                childLocationViewModel = childLocationViewModel
            )

            FloatingActionButton(
                onClick = { trackingStarted = !trackingStarted },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Text(if (trackingStarted) "Stop" else "Start")
            }
        }
    }
}

// ==========================
// Dashboard & Drawer
// ==========================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardWithDrawer(
    navController: NavHostController,
    dangerZoneViewModel: DangerZoneViewModel,
    childUID: String,
    parentId: String
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(modifier = Modifier.padding(16.dp)) {

                // Drawer Close Button
                IconButton(onClick = { scope.launch { drawerState.close() } }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = androidx.compose.ui.graphics.Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Dashboard Menu",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ----------------------------
                // Manage Danger Zones Button
                // ----------------------------
                Button(
                    onClick = {
                        navController.navigate("danger_zone/$parentId/$childUID")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage Danger Zones")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ----------------------------
                // ⭐ Child Safety Insights Button
                // (Styled exactly the same)
                // ----------------------------
                Button(
                    onClick = {
                        navController.navigate("insights/$parentId/$childUID")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Child Safety Insights")
                }
            }
        }
    ) {
        DashboardScreen(
            navController = navController,
            parentId = parentId,
            childUID = childUID,
            dangerZoneViewModel = dangerZoneViewModel,
            profileImageUri = null,
            onProfileClick = { scope.launch { drawerState.open() } },
            onImagePick = {}
        )
    }
}



// ==========================
// DashboardScreen (YOU WANTED VERSION: FAB starts/stops DangerZoneService)
// ==========================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    parentId: String?,
    childUID: String?,
    profileImageUri: String?,
    onProfileClick: () -> Unit,
    onImagePick: () -> Unit,
    dangerZoneViewModel: DangerZoneViewModel
) {
    val context = LocalContext.current
    var tracking by remember { mutableStateOf(false) }

    val childLocationViewModel: ChildLocationViewModel = viewModel()
    val alertViewModel: DangerZoneAlertViewModel = viewModel()
    val status by alertViewModel.zoneStatus

    // Firestore listener
    LaunchedEffect(childUID) {
        if (!childUID.isNullOrBlank() && !parentId.isNullOrBlank()) {
            alertViewModel.startListening(parentId, childUID)
        }
    }

    // Launcher for Notification permission (Android 13+)
    val requestNotificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Notification permission is required", Toast.LENGTH_LONG).show()
        }
    }

    // Launcher for Location & Foreground service permissions
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineLocationGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val foregroundGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            perms[Manifest.permission.FOREGROUND_SERVICE_LOCATION] == true
        } else true

        if (fineLocationGranted && foregroundGranted) {
            // Permissions granted, start service
            startDangerZoneService(context, childUID!!, parentId!!)
            tracking = true
            Toast.makeText(context, "Tracking started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
            tracking = false
        }
    }

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

            // Map + tracking content
            if (!childUID.isNullOrBlank() && !parentId.isNullOrBlank()) {
                TrackingContentSafe(
                    parentId = parentId,
                    childUID = childUID,
                    trackingStarted = tracking,
                    dangerZoneViewModel = dangerZoneViewModel,
                    childLocationViewModel = childLocationViewModel
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading user info…")
                }
            }

            // Danger zone alert box
            Column(modifier = Modifier.align(Alignment.TopCenter)) {
                DangerZoneAlertBox(status)
            }

            // Start/Stop FAB
            FloatingActionButton(
                onClick = {
                    // Safety: Check IDs
                    if (childUID.isNullOrBlank() || parentId.isNullOrBlank()) {
                        Toast.makeText(context, "User data not ready", Toast.LENGTH_SHORT).show()
                        return@FloatingActionButton
                    }

                    // Notification permission (Android 13+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return@FloatingActionButton
                    }

                    if (!tracking) {
                        // Prepare permissions to request
                        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
                        }

                        // Check if permissions already granted
                        val hasAllPermissions = permissions.all { perm ->
                            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
                        }

                        if (hasAllPermissions) {
                            startDangerZoneService(context, childUID, parentId)
                            tracking = true
                            Toast.makeText(context, "Tracking started", Toast.LENGTH_SHORT).show()
                        } else {
                            // Request missing permissions
                            locationLauncher.launch(permissions.toTypedArray())
                        }

                    } else {
                        // Stop tracking
                        context.stopService(Intent(context, DangerZoneService::class.java))
                        tracking = false
                        Toast.makeText(context, "Tracking stopped", Toast.LENGTH_SHORT).show()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "Start/Stop Tracking")
            }
        }
    }
}

// Helper function to start the foreground service safely
private fun startDangerZoneService(context: Context, childUID: String, parentId: String) {
    val intent = Intent(context, DangerZoneService::class.java).apply {
        putExtra(DangerZoneService.CHILD_UID, childUID)
        putExtra("parentId", parentId)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}



// ==========================
// BottomNavigationBar
// ==========================
@Composable
fun BottomNavigationBar() {
    NavigationBar {
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.Email, contentDescription = null) })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.LocationOn, contentDescription = null) })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.Person, contentDescription = null) })
    }
}

// ==========================
// DashboardMapView
// ==========================
@Composable
fun DashboardMapView(
    current: Location,
    history: List<LocationLog>,
    storeMarkers: List<StorePOI> = emptyList(),
    childUID: String,
    dangerZoneViewModel: DangerZoneViewModel
) {
    val dangerZones by remember { derivedStateOf { dangerZoneViewModel.dangerZones.toList() } }

    // Keep track of last camera position to avoid jumping
    val lastCameraPosition = remember { mutableStateOf<GeoPoint?>(null) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isClickable = true
                setBuiltInZoomControls(true)
                controller.setZoom(18.0)
                controller.setCenter(GeoPoint(current.latitude, current.longitude))
                lastCameraPosition.value = GeoPoint(current.latitude, current.longitude)
            }
        },
        update = { map ->
            map.overlays.clear()

            // Draw location history polyline
            val points = history.map { GeoPoint(it.latitude, it.longitude) }
            if (points.isNotEmpty()) map.overlays.add(Polyline().apply { setPoints(points) })

            // Add child marker
            map.overlays.add(Marker(map).apply {
                position = GeoPoint(current.latitude, current.longitude)
                title = "Current location"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            })

            // Add danger zones
            dangerZones.forEach { zone ->
                map.overlays.add(Polygon().apply {
                    setPoints(createCirclePoints(zone.lat, zone.lon, zone.radius))
                    fillColor = androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.2f).toArgb()
                    strokeColor = androidx.compose.ui.graphics.Color.Red.toArgb()
                    strokeWidth = 2f
                })
            }

            // Only move camera if the location changed
            val newGeo = GeoPoint(current.latitude, current.longitude)
            if (lastCameraPosition.value == null ||
                lastCameraPosition.value!!.latitude != newGeo.latitude ||
                lastCameraPosition.value!!.longitude != newGeo.longitude
            ) {
                map.controller.setCenter(newGeo)
                lastCameraPosition.value = newGeo
            }

            map.invalidate()
        }
    )
}


fun createCirclePoints(lat: Double, lon: Double, radiusMeters: Double, pointsCount: Int = 36): List<GeoPoint> {
    val result = mutableListOf<GeoPoint>()
    val radiusDegrees = radiusMeters / 111_320.0
    for (i in 0 until pointsCount) {
        val angle = 2 * PI * i / pointsCount
        val dx = radiusDegrees * cos(angle)
        val dy = radiusDegrees * sin(angle)
        result.add(GeoPoint(lat + dy, lon + dx))
    }
    return result
}
