package com.example.kidscontrolapp.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.kidscontrolapp.utils.FirestoreProvider
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class ZoneStateItem(
    val zoneId: String,
    val name: String?,
    val state: String,
    val duration: String?
)

data class AlertItem(
    val timestamp: Long,
    val message: String,
    val zoneId: String?
)

class ChildInsightsViewModel : ViewModel() {

    private val db = FirestoreProvider.getFirestore()

    val lat = mutableStateOf<Double?>(null)
    val lon = mutableStateOf<Double?>(null)
    val battery = mutableStateOf<Double?>(null)
    val speed = mutableStateOf<Double?>(null)
    val lastUpdated = mutableStateOf("--")

    val zoneStates = mutableStateListOf<ZoneStateItem>()
    val alerts = mutableStateListOf<AlertItem>()

    private var childListener: ListenerRegistration? = null
    private var alertsListener: ListenerRegistration? = null

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun start(parentId: String, childUid: String) {
        stop()

        // Listen child_position
        childListener = db.collection("child_position")
            .document(childUid)
            .addSnapshotListener { doc, err ->
                if (err != null || doc == null || !doc.exists()) return@addSnapshotListener

                val data = doc.data ?: return@addSnapshotListener

                lat.value = (data["lat"] as? Number)?.toDouble()
                lon.value = (data["lon"] as? Number)?.toDouble()
                battery.value = (data["battery"] as? Number)?.toDouble()
                speed.value = (data["speed"] as? Number)?.toDouble()

                val ts = (data["lastUpdatedRaw"] as? Number)?.toLong()
                lastUpdated.value = if (ts != null) dateFmt.format(Date(ts)) else "--"

                // Zone states map
                val states = data["zoneStates"] as? Map<*, *>
                val durations = data["timeInZone"] as? Map<*, *>

                zoneStates.clear()
                states?.forEach { (zoneId, status) ->
                    zoneStates.add(
                        ZoneStateItem(
                            zoneId = zoneId.toString(),
                            name = null,
                            state = status.toString(),
                            duration = durations?.get(zoneId)?.toString()
                        )
                    )
                }
            }

        // Listen alerts
        alertsListener = db.collection("notifications_log")
            .document(parentId)
            .addSnapshotListener { doc, _ ->
                if (doc == null || !doc.exists()) return@addSnapshotListener

                val arr = doc.get("notifications") as? List<Map<String, Any>> ?: return@addSnapshotListener

                alerts.clear()
                arr.reversed().take(50).forEach { item ->
                    val ts = (item["timestamp"] as? Long)
                        ?: System.currentTimeMillis()

                    alerts.add(
                        AlertItem(
                            timestamp = ts,
                            message = item["message"]?.toString() ?: "Alert",
                            zoneId = item["zoneId"]?.toString()
                        )
                    )
                }
            }

        Log.d("INSIGHT_VM", "Listening for child=$childUid parent=$parentId")
    }

    fun stop() {
        childListener?.remove()
        alertsListener?.remove()
        childListener = null
        alertsListener = null
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}

fun Long.toAgo(): String {
    val diff = System.currentTimeMillis() - this
    return when {
        diff < 60000 -> "${diff / 1000}s ago"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}
