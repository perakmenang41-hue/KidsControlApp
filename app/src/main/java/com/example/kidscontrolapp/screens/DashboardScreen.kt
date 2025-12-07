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
// ==========================
@Composable
fun TrackingContentSafe(
    parentId: String = "",
    childUID: String = "",
    trackingStarted: Boolean = false,
    dangerZoneViewModel: DangerZoneViewModel = viewModel(),
    childLocationViewModel: ChildLocationViewModel = viewModel()
) {
    val firestore = FirestoreProvider.getFirestore()
    val context = LocalContext.current

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    val locationHistory = remember { mutableStateListOf<LocationLog>() }
    var batteryLevel by remember { mutableStateOf(0.0) }
    var status by remember { mutableStateOf("offline") }

    val dangerZones by remember { derivedStateOf { dangerZoneViewModel.dangerZones.toList() } }

    // Timer for periodic Firestore polling
    LaunchedEffect(childUID) {
        while (true) {
            try {
                val snapshot = firestore.collection("child_position")
                    .document(childUID)
                    .get()
                    .await()

                if (snapshot.exists()) {
                    val lat = snapshot.getDouble("lat")
                    val lon = snapshot.getDouble("lon")
                    val battery = snapshot.getDouble("battery") ?: 0.0
                    val childStatus = snapshot.getString("status") ?: "offline"

                    if (lat != null && lon != null) {
                        val loc = Location("").apply { latitude = lat; longitude = lon }
                        currentLocation = loc
                        locationHistory.add(LocationLog(lat, lon, battery))
                    }

                    batteryLevel = battery
                    status = childStatus
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error fetching location: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            // Wait 5 seconds before next fetch
            kotlinx.coroutines.delay(5000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        currentLocation?.let { loc ->
            DashboardMapView(
                current = loc,
                history = locationHistory.toList(),
                storeMarkers = emptyList(),
                childUID = childUID,
                dangerZoneViewModel = dangerZoneViewModel
            )
        } ?: Text(
            "Waiting for location…",
            modifier = Modifier.align(Alignment.Center)
        )

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
    parentId: String // <-- pass the logged-in parentId here
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var profileImageUri by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(modifier = Modifier.padding(16.dp)) {
                IconButton(onClick = { scope.launch { drawerState.close() } }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = androidx.compose.ui.graphics.Color.Black)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .clickable { }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Child Name", style = MaterialTheme.typography.titleMedium)
                    Text("UID: ${childUID ?: "N/A"}", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { }) { Text("Edit Profile") }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        // Navigate to DangerZoneScreen and pass parentId
                        navController.navigate("danger_zone/$parentId")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Manage Danger Zones") }
            }
        }
    ) {
        childUID?.let { uid ->
            DashboardScreen(
                navController = navController,
                parentId = parentId, // <-- pass the parentId here
                childUID = uid,
                profileImageUri = profileImageUri,
                onProfileClick = { scope.launch { drawerState.open() } },
                onImagePick = { },
                dangerZoneViewModel = dangerZoneViewModel
            )
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Waiting for UID…")
        }
    }
}

// ==========================
// DashboardScreen
// ==========================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    parentId: String,
    childUID: String,
    profileImageUri: String?,
    onProfileClick: () -> Unit,
    onImagePick: () -> Unit,
    dangerZoneViewModel: DangerZoneViewModel
) {
    val context = LocalContext.current
    var tracking by remember { mutableStateOf(false) }

    // ViewModels
    val childLocationViewModel: ChildLocationViewModel = viewModel()
    val alertViewModel: DangerZoneAlertViewModel = viewModel()

    // Notification permission launcher
    val requestNotificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Notification permission is required", Toast.LENGTH_LONG).show()
        }
    }

    // Start Firestore listener for alerts
    val status by alertViewModel.zoneStatus
    LaunchedEffect(childUID) {
        alertViewModel.startListening(parentId, childUID)
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

            // ----------------------
            // Map + Tracking
            // ----------------------
            TrackingContentSafe(
                childUID = childUID,
                trackingStarted = tracking,
                dangerZoneViewModel = dangerZoneViewModel,
                childLocationViewModel = childLocationViewModel
            )

            // ----------------------
            // Floating DangerZone Alert
            // ----------------------
            Column(modifier = Modifier.align(Alignment.TopCenter)) {
                DangerZoneAlertBox(status)
            }

            // ----------------------
            // Floating Action Button (Start/Stop Tracking)
            // ----------------------
            FloatingActionButton(
                onClick = {
                    // Notification permission check
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return@FloatingActionButton
                    }

                    if (childUID.isBlank()) return@FloatingActionButton

                    tracking = !tracking
                    val intent = Intent(context, DangerZoneService::class.java).apply {
                        putExtra(DangerZoneService.CHILD_UID, childUID)
                        putParcelableArrayListExtra(
                            DangerZoneService.ZONES,
                            ArrayList(dangerZoneViewModel.dangerZones.toList())
                        )
                    }

                    if (tracking) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                        else context.startService(intent)
                        Toast.makeText(context, "Tracking started", Toast.LENGTH_SHORT).show()
                    } else {
                        context.stopService(Intent(context, DangerZoneService::class.java))
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
            }
        },
        update = { map ->
            map.overlays.clear()
            val points = history.map { GeoPoint(it.latitude, it.longitude) }
            if (points.isNotEmpty()) map.overlays.add(Polyline().apply { setPoints(points) })

            map.overlays.add(Marker(map).apply {
                position = GeoPoint(current.latitude, current.longitude)
                title = "Current location"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            })
            map.controller.setCenter(GeoPoint(current.latitude, current.longitude))

            dangerZones.forEach { zone ->
                map.overlays.add(Polygon().apply {
                    setPoints(createCirclePoints(zone.lat, zone.lon, zone.radius))
                    fillColor = androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.2f).toArgb()
                    strokeColor = androidx.compose.ui.graphics.Color.Red.toArgb()
                    strokeWidth = 2f
                })
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
