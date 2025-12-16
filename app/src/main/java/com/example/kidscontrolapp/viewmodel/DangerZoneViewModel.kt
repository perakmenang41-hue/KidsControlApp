package com.example.kidscontrolapp.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.kidscontrolapp.utils.FirestoreProvider
import com.example.kidscontrolapp.model.DangerZone
import com.example.kidscontrolapp.network.DangerZoneResponse
import com.example.kidscontrolapp.network.DeleteZoneRequest
import com.example.kidscontrolapp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DangerZoneViewModel : ViewModel() {

    private val db = FirestoreProvider.getFirestore()
    private val _dangerZones = mutableStateListOf<DangerZone>()
    val dangerZones: List<DangerZone> get() = _dangerZones

    fun addZone(zone: DangerZone) {
        _dangerZones.add(zone)
    }

    fun removeZone(zone: DangerZone) {
        _dangerZones.remove(zone)
    }

    fun loadDangerZones(parentId: String) {
        db.collection("Parent_registered")
            .document(parentId)
            .collection("dangerZones")
            .get()
            .addOnSuccessListener { snapshot ->
                _dangerZones.clear()
                snapshot.forEach { doc ->
                    val data = doc.data
                    try {
                        _dangerZones.add(
                            DangerZone(
                                id = doc.id,
                                name = data["name"].toString(),
                                lat = (data["lat"]?.toString() ?: "0.0").toDouble(),
                                lon = (data["lon"]?.toString() ?: "0.0").toDouble(),
                                radius = (data["radius"]?.toString() ?: "50.0").toDouble()
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("DANGER_ZONE", "Error parsing zone ${doc.id}", e)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("DANGER_ZONE", "Failed to load danger zones", e)
            }
    }

    fun deleteDangerZone(
        context: Context,
        parentId: String,
        zoneId: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        // 1️⃣ Delete from Parent_registered/dangerZones
        db.collection("Parent_registered")
            .document(parentId)
            .collection("dangerZones")
            .document(zoneId)
            .delete()
            .addOnSuccessListener {
                _dangerZones.removeAll { it.id == zoneId }
                Toast.makeText(context, "Zone deleted successfully", Toast.LENGTH_SHORT).show()
                onSuccess?.invoke()

                // 2️⃣ Remove zone references from all child_position maps
                removeZoneFromChildrenMaps(parentId, zoneId)
            }
            .addOnFailureListener { e ->
                Log.e("DANGER_ZONE", "Failed to remove zone", e)
                Toast.makeText(context, "Failed to remove zone: ${e.message}", Toast.LENGTH_SHORT).show()
                onError?.invoke(e.message ?: "Unknown error")
            }

        // 3️⃣ Delete from backend API
        val request = DeleteZoneRequest(parentId)
        RetrofitClient.api.removeDangerZone(zoneId, request)
            .enqueue(object : Callback<DangerZoneResponse> {
                override fun onResponse(call: Call<DangerZoneResponse>, response: Response<DangerZoneResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Log.d("DANGER_ZONE", "Zone removed from backend")
                    } else {
                        Log.e("DANGER_ZONE", "Failed to remove zone from backend")
                    }
                }

                override fun onFailure(call: Call<DangerZoneResponse>, t: Throwable) {
                    Log.e("DANGER_ZONE", "Network error removing zone", t)
                }
            })
    }

    // ============================================================
    // Remove zone ID from all relevant child_position maps
    // ============================================================
    private fun removeZoneFromChildrenMaps(parentId: String, zoneId: String) {
        db.collection("child_position")
            .whereEqualTo("parentId", parentId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.forEach { doc ->
                    val docRef = doc.reference
                    val timeInZone = doc.get("timeInZone") as? MutableMap<String, Any> ?: mutableMapOf()
                    val timeInZoneRaw = doc.get("timeInZoneRaw") as? MutableMap<String, Any> ?: mutableMapOf()
                    val zoneStates = doc.get("zoneStates") as? MutableMap<String, Any> ?: mutableMapOf()

                    timeInZone.remove(zoneId)
                    timeInZoneRaw.remove(zoneId)
                    zoneStates.remove(zoneId)

                    batch.update(docRef,
                        "timeInZone", timeInZone,
                        "timeInZoneRaw", timeInZoneRaw,
                        "zoneStates", zoneStates
                    )
                }
                batch.commit()
                    .addOnSuccessListener { Log.d("DANGER_ZONE", "Zone removed from all children maps") }
                    .addOnFailureListener { e -> Log.e("DANGER_ZONE", "Failed to remove zone from children maps", e) }
            }
            .addOnFailureListener { e -> Log.e("DANGER_ZONE", "Failed to query children for zone deletion", e) }
    }
}

