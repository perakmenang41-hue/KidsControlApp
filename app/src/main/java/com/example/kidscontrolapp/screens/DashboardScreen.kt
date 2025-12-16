package com.example.kidscontrolapp.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Parcelable
import android.widget.Toast
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue                // <-- needed for “by” delegates
import androidx.compose.runtime.setValue                // <-- only needed for mutable delegates
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource           // <-- localisation helper
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.kidscontrolapp.R                 // <-- generated R class
import com.example.kidscontrolapp.components.DangerZoneAlertBox
import com.example.kidscontrolapp.components.TopBar
import com.example.kidscontrolapp.viewmodel.DangerZoneAlertViewModel
import com.example.kidscontrolapp.viewmodel.DangerZoneViewModel
import com.example.kidscontrolapp.viewmodel.ChildLocationViewModel
import com.example.kidscontrolapp.service.DangerZoneService
import com.example.kidscontrolapp.utils.FirestoreProvider
import com.example.kidscontrolapp.data.DataStoreHelper
import com.example.kidscontrolapp.utils.createCirclePoints
import com.example.kidscontrolapp.viewmodel.LocaleViewModel
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

// -------------------------------------------------------------------
// Data classes
// -------------------------------------------------------------------
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

// -------------------------------------------------------------------
// Permission requester
// -------------------------------------------------------------------
@Composable
fun LocationPermissionRequester(localeViewModel: LocaleViewModel, onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current
    val locale by localeViewModel.locale.collectAsState()
    val localizedContext = remember(locale) {
        val config = context.resources.configuration
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            onPermissionsGranted()
        } else {
            Toast.makeText(
                context,
                localizedContext.getString(R.string.toast_location_permission_required),
                Toast.LENGTH_SHORT
            ).show()
        }
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
// -------------------------------------------------------------------
// TrackingContentSafe – UI that only shows the map when tracking is on
// -------------------------------------------------------------------
@Composable
fun TrackingContentSafe(
    parentId: String,
    childUID: String,
    trackingStarted: Boolean = false,
    dangerZoneViewModel: DangerZoneViewModel = viewModel(),
    childLocationViewModel: ChildLocationViewModel = viewModel(),
    localeViewModel: LocaleViewModel
) {
    val context = LocalContext.current
    val locale by localeViewModel.locale.collectAsState()
    val localizedContext = remember(locale) {
        val config = context.resources.configuration
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    val locationHistory = remember { mutableStateListOf<LocationLog>() }
    var batteryLevel by remember { mutableStateOf(0.0) }
    var status by remember { mutableStateOf("offline") }
    var listener: ListenerRegistration? by remember { mutableStateOf(null) }

    val dangerZones by remember { derivedStateOf { dangerZoneViewModel.dangerZones.toList() } }
    val lastValidCameraPosition = remember { mutableStateOf<Location?>(null) }

    fun Location.isValid(): Boolean =
        !(latitude == 0.0 && longitude == 0.0) &&
                !(latitude == 37.4219983 && longitude == -122.084)

    LaunchedEffect(trackingStarted, childUID) {
        listener?.remove()
        listener = null

        if (trackingStarted) {
            try {
                FirestoreProvider.getFirestore().collection("child_position")
                    .document(childUID)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener
                        if (snapshot != null && snapshot.exists()) {
                            val lat = snapshot.getDouble("lat")
                            val lon = snapshot.getDouble("lon")
                            val batt = snapshot.getDouble("battery") ?: 0.0
                            val childStatus = snapshot.getString("status") ?: "offline"

                            if (lat != null && lon != null) {
                                val loc = Location("").apply {
                                    latitude = lat
                                    longitude = lon
                                }
                                if (loc.isValid()) {
                                    currentLocation = loc
                                    lastValidCameraPosition.value = loc
                                    locationHistory.add(LocationLog(lat, lon, batt))
                                }
                            }
                            batteryLevel = batt
                            status = childStatus
                        }
                    }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "${localizedContext.getString(R.string.toast_error)}: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            currentLocation = null
            locationHistory.clear()
            batteryLevel = 0.0
            status = "offline"
            lastValidCameraPosition.value = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            listener?.remove()
            listener = null
        }
    }

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.02f)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.LocationOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = localizedContext.getString(R.string.msg_tracking_off),
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            // 🔋 Battery row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = when {
                        batteryLevel >= 60 -> Icons.Default.BatteryFull
                        batteryLevel >= 30 -> Icons.Default.Battery6Bar
                        batteryLevel >= 15 -> Icons.Default.Battery3Bar
                        else -> Icons.Default.BatteryAlert
                    },
                    contentDescription = null,
                    tint = when {
                        batteryLevel >= 30 -> Color(0xFF4CAF50)   // green
                        batteryLevel >= 15 -> Color(0xFFFFC107)   // amber
                        else -> Color(0xFFF44336)                 // red
                    },
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = "${batteryLevel.toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // 🟢🔴 Status row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = null,
                    tint = if (status == "online")
                        Color(0xFF4CAF50)
                    else
                        Color(0xFFF44336),
                    modifier = Modifier.size(12.dp)
                )

                Text(
                    text = if (status == "online")
                        localizedContext.getString(R.string.status_online)
                    else
                        localizedContext.getString(R.string.status_offline),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// -------------------------------------------------------------------
// DashboardAutoTrackingScreen – loads stored IDs then shows TrackingContentSafe
// -------------------------------------------------------------------
@Composable
fun DashboardAutoTrackingScreen(
    navController: NavHostController,
    dangerZoneViewModel: DangerZoneViewModel = viewModel(),
    childLocationViewModel: ChildLocationViewModel = viewModel()
) {
    val localeViewModel: LocaleViewModel = viewModel()
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

    // You can change this default to true if you want auto‑start
    LaunchedEffect(parentId, childUID) {
        if (!parentId.isNullOrBlank() && !childUID.isNullOrBlank()) {
            trackingStarted = false
        }
    }

    if (parentId != null && childUID != null) {
        TrackingContentSafe(
            parentId = parentId!!,
            childUID = childUID!!,
            trackingStarted = trackingStarted,
            dangerZoneViewModel = dangerZoneViewModel,
            childLocationViewModel = childLocationViewModel,
            localeViewModel = localeViewModel       // ✅ pass it here
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.msg_loading_user_info))
        }
    }
}

// -------------------------------------------------------------------
// TrackingContentWrapper – permission request + start/stop FAB
// -------------------------------------------------------------------
@Composable
fun TrackingContentWrapper(
    parentId: String,
    childUID: String,
    dangerZoneViewModel: DangerZoneViewModel,
    childLocationViewModel: ChildLocationViewModel,
    localeViewModel: LocaleViewModel
) {
    var permissionsGranted by remember { mutableStateOf(false) }
    var trackingStarted by remember { mutableStateOf(false) }

    if (!permissionsGranted) {
        LocationPermissionRequester(localeViewModel) {
            permissionsGranted = true
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            TrackingContentSafe(
                parentId = parentId!!,
                childUID = childUID!!,
                trackingStarted = trackingStarted,
                dangerZoneViewModel = dangerZoneViewModel,
                childLocationViewModel = childLocationViewModel,
                localeViewModel = localeViewModel       // ✅ pass it here
            )

            FloatingActionButton(
                onClick = { trackingStarted = !trackingStarted },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Text(
                    text = if (trackingStarted)
                        stringResource(R.string.btn_stop)
                    else
                        stringResource(R.string.btn_start)
                )
            }
        }
    }
}

// -------------------------------------------------------------------
// Dashboard & Drawer
// -------------------------------------------------------------------
@Composable
fun DashboardWithDrawer(
    navController: NavHostController,
    dangerZoneViewModel: DangerZoneViewModel,
    childUID: String,
    parentId: String,
    profileImageUri: Uri?,
    onProfileImageChange: (Uri?) -> Unit
) {
    val localeViewModel: LocaleViewModel = viewModel()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()


    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            Column(modifier = Modifier.padding(16.dp)) {

                IconButton(onClick = { scope.launch { drawerState.close() } }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.content_desc_back)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.title_dashboard_menu),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { navController.navigate("danger_zone/$parentId/$childUID") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.btn_manage_danger_zones))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { navController.navigate("insights/$parentId/$childUID") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.btn_child_safety_insights))
                }
            }
        }
    ) {
        DashboardScreen(
            navController = navController,
            parentId = parentId,
            childUID = childUID,
            dangerZoneViewModel = dangerZoneViewModel,
            profileImageUri = profileImageUri,
            onProfileClick = { scope.launch { drawerState.open() } }, // ✅ ONLY WAY TO OPEN
            onImagePick = onProfileImageChange,
            localeViewModel = localeViewModel     // ✅ pass it here!
        )
    }
}


// -------------------------------------------------------------------
// DashboardScreen – the screen that shows the map, alerts and the FAB
// -------------------------------------------------------------------


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    parentId: String,
    childUID: String,
    dangerZoneViewModel: DangerZoneViewModel,
    profileImageUri: Uri?,
    // <-- the click that will open the drawer
    onProfileClick: () -> Unit,
    onImagePick: (Uri?) -> Unit,
    localeViewModel: LocaleViewModel
) {
    // -----------------------------------------------------------------
    // 1️⃣ ViewModels & UI state
    // -----------------------------------------------------------------
    val childLocationViewModel: ChildLocationViewModel = viewModel()
    val alertViewModel: DangerZoneAlertViewModel = viewModel()
    val status by alertViewModel.zoneStatus
    var tracking by remember { mutableStateOf(false) }

    // -----------------------------------------------------------------
    // 2️⃣ Drawer state (single source of truth for this screen)
    // -----------------------------------------------------------------
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // -----------------------------------------------------------------
    // 3️⃣ Locale handling (keeps your existing localisation logic)
    // -----------------------------------------------------------------
    val context = LocalContext.current
    val locale by localeViewModel.locale.collectAsState()
    val localizedContext = remember(locale) {
        val cfg = context.resources.configuration
        cfg.setLocale(locale)
        context.createConfigurationContext(cfg)
    }

    // -----------------------------------------------------------------
    // 4️⃣ Start listening for danger‑zone updates
    // -----------------------------------------------------------------
    LaunchedEffect(childUID) {
        alertViewModel.startListening(parentId, childUID)
    }

    // -----------------------------------------------------------------
    // 5️⃣ **Wrap everything in a single ModalNavigationDrawer**
    // -----------------------------------------------------------------
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,   // you can swipe to open if you like
        drawerContent = {
            Column(modifier = Modifier.padding(16.dp)) {
                // ---- Close button (top‑left) ----
                IconButton(onClick = { scope.launch { drawerState.close() } }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = localizedContext.getString(R.string.content_desc_back)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ---- Drawer title ----
                Text(
                    text = localizedContext.getString(R.string.title_dashboard_menu),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ---- Manage Danger Zones button ----
                Button(
                    onClick = {
                        // Close drawer first (optional) then navigate
                        scope.launch { drawerState.close() }
                        navController.navigate("danger_zone/$parentId/$childUID")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(localizedContext.getString(R.string.btn_manage_danger_zones))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ---- Child‑Safety Insights button ----
                Button(
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("insights/$parentId/$childUID")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(localizedContext.getString(R.string.btn_child_safety_insights))
                }
            }
        }
    ) {
        // -----------------------------------------------------------------
        // 6️⃣ Scaffold (TopBar + main content)
        // -----------------------------------------------------------------
        Scaffold(
            topBar = {
                TopBar(
                    title = localizedContext.getString(R.string.title_dashboard),
                    navController = navController,
                    profileImage = profileImageUri,
                    // **When the avatar is tapped we open the drawer**
                    onProfileClick = {
                        // This lambda is the same `onProfileClick` you passed in,
                        // but we also forward it to the drawerState here.
                        scope.launch { drawerState.open() }
                        // If you need to do any extra work you can also call the
                        // original `onProfileClick` passed from the caller:
                        onProfileClick()
                    },
                    onImagePick = onImagePick,
                    showBackButton = false
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // ---- Main map / tracking UI ----
                TrackingContentSafe(
                    parentId = parentId,
                    childUID = childUID,
                    trackingStarted = tracking,
                    dangerZoneViewModel = dangerZoneViewModel,
                    childLocationViewModel = childLocationViewModel,
                    localeViewModel = localeViewModel
                )

                // ---- Danger‑zone alert banner (top‑center) ----
                Column(modifier = Modifier.align(Alignment.TopCenter)) {
                    DangerZoneAlertBox(status)
                }

                // ---- Start/Stop tracking FAB (bottom‑right) ----
                FloatingActionButton(
                    onClick = { tracking = !tracking },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 72.dp)
                ) {
                    Icon(
                        imageVector = if (tracking) Icons.Default.Stop else Icons.Default.LocationOn,
                        contentDescription = localizedContext.getString(R.string.content_desc_start_stop_tracking)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------
// Helper – start the foreground service safely (unchanged)
// -------------------------------------------------------------------
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

// -------------------------------------------------------------------
// DashboardMapView – unchanged except for imports already added above
// -------------------------------------------------------------------
@Composable
fun DashboardMapView(
    current: Location,
    history: List<LocationLog>,
    storeMarkers: List<StorePOI> = emptyList(),
    childUID: String,
    dangerZoneViewModel: DangerZoneViewModel
) {
    val dangerZones by remember { derivedStateOf { dangerZoneViewModel.dangerZones.toList() } }

    // Keep track of the last camera position so the map doesn’t jump
    val lastCameraPosition = remember { mutableStateOf<GeoPoint?>(null) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        // -------------------------------------------------
        // 1️⃣  Create the MapView – we keep a reference to the Context (ctx)
        // -------------------------------------------------
        factory = { ctx ->
            Configuration.getInstance()
                .load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

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
        // -------------------------------------------------
        // 2️⃣  **Never call a composable (e.g. stringResource) here**
        // -------------------------------------------------
        update = { map ->
            map.overlays.clear()

            // ---- Draw the location‑history polyline ----
            val points = history.map { GeoPoint(it.latitude, it.longitude) }
            if (points.isNotEmpty()) {
                map.overlays.add(Polyline().apply { setPoints(points) })
            }

            // ---- Add the child marker ----
            // Use the Context that belongs to the MapView to fetch a plain String
            val markerTitle = map.context.getString(R.string.marker_current_location)

            map.overlays.add(
                Marker(map).apply {
                    position = GeoPoint(current.latitude, current.longitude)
                    title = markerTitle               // <-- non‑composable string
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            )

            // ---- Add danger‑zone polygons ----
            dangerZones.forEach { zone ->
                map.overlays.add(
                    Polygon().apply {
                        setPoints(createCirclePoints(zone.lat, zone.lon, zone.radius))
                        fillColor = androidx.compose.ui.graphics.Color.Red
                            .copy(alpha = 0.2f).toArgb()
                        strokeColor = androidx.compose.ui.graphics.Color.Red.toArgb()
                        strokeWidth = 2f
                    }
                )
            }

            // ---- Move the camera only when the location really changed ----
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

// -------------------------------------------------------------------
// Helper – generate points for a circular polygon
// -------------------------------------------------------------------
fun createCirclePoints(
    lat: Double,
    lon: Double,
    radiusMeters: Double,
    pointsCount: Int = 36
): List<GeoPoint> {
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