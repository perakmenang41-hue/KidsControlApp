package com.example.kidscontrolapp.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.kidscontrolapp.utils.FirestoreProvider
import com.example.kidscontrolapp.model.DangerZone
import com.example.kidscontrolapp.network.DangerZoneResponse
import com.example.kidscontrolapp.network.RetrofitClient
import com.example.kidscontrolapp.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DangerZoneViewModel : ViewModel() {

    // Firestore instance (auto uses emulator in Debug)
    private val db = FirestoreProvider.getFirestore()


    // Local zone list
    private val _dangerZones = mutableStateListOf<DangerZone>()
    val dangerZones: List<DangerZone> get() = _dangerZones

    fun addZone(zone: DangerZone) {
        _dangerZones.add(zone)
    }

    fun removeZone(zone: DangerZone) {
        _dangerZones.remove(zone)
    }

    // ============================================================
    // LOAD ALL DANGER ZONES FROM PARENT SUBCOLLECTION
    // ============================================================
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
                                lat = data["lat"] as Double,
                                lon = data["lon"] as Double,
                                radius = data["radius"] as Double
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("DANGER_ZONE", "Error parsing zone ${doc.id}", e)
                    }
                }
                Log.d("DANGER_ZONE", "✅ Loaded ${_dangerZones.size} zones for parent $parentId")
            }
            .addOnFailureListener { e ->
                Log.e("DANGER_ZONE", "❌ Failed to load danger zones", e)
            }
    }

    // ============================================================
    // ADD ZONE TO FIRESTORE UNDER PARENT
    // ============================================================
    fun addZoneToParent(parentId: String, zone: DangerZone) {
        val parentDoc = db.collection("Parent_registered").document(parentId)
        parentDoc.collection("dangerZones")
            .document(zone.id)
            .set(zone)
            .addOnSuccessListener {
                Log.d("DANGER_ZONE", "✅ Zone added to parent $parentId with id ${zone.id}")
                _dangerZones.add(zone)
            }
            .addOnFailureListener { e ->
                Log.e("DANGER_ZONE", "❌ Failed to add zone", e)
            }
    }

    // ============================================================
    // ADD ZONE TO BACKEND (optional)
    // ============================================================
    fun addZoneToBackend(
        context: Context,
        parentId: String,
        name: String,
        lat: Double,
        lon: Double,
        radius: Double,
        onComplete: (Boolean) -> Unit
    ) {
        val zoneData = hashMapOf(
            "name" to name,
            "lat" to lat,
            "lon" to lon,
            "radius" to radius,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("Parent_registered")
            .document(parentId)
            .collection("dangerZones")
            .add(zoneData)
            .addOnSuccessListener {
                _dangerZones.add(
                    DangerZone(
                        id = it.id,
                        name = name,
                        lat = lat,
                        lon = lon,
                        radius = radius
                    )
                )
                onComplete(true)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to add danger zone: ${it.message}", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
    }

    // ============================================================
    // REMOVE ZONE FROM BACKEND
    // ============================================================
    fun removeZoneFromBackend(
        context: Context,
        parentId: String,
        zoneId: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        db.collection("Parent_registered")
            .document(parentId)
            .collection("dangerZones")
            .document(zoneId)
            .delete()
            .addOnSuccessListener {
                Log.d("DANGER_ZONE", "🗑 Zone removed from Firestore")
                _dangerZones.removeAll { it.id == zoneId }
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                Log.e("DANGER_ZONE", "❌ Failed to remove zone", e)
                Toast.makeText(context, "Failed to remove zone: ${e.message}", Toast.LENGTH_SHORT).show()
                onComplete?.invoke(false)
            }

        RetrofitClient.api.removeDangerZone(zoneId)
            .enqueue(object : Callback<DangerZoneResponse> {
                override fun onResponse(call: Call<DangerZoneResponse>, response: Response<DangerZoneResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Log.d("DANGER_ZONE", "✅ Zone removed from backend")
                    } else {
                        Log.e("DANGER_ZONE", "❌ Failed to remove zone from backend")
                    }
                }

                override fun onFailure(call: Call<DangerZoneResponse>, t: Throwable) {
                    Log.e("DANGER_ZONE", "❌ Network error removing zone", t)
                }
            })
    }
}
