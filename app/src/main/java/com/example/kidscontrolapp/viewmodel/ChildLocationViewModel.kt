package com.example.kidscontrolapp.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.kidscontrolapp.network.GenericResponse
import com.example.kidscontrolapp.network.RetrofitClient
import com.example.kidscontrolapp.network.UpdateLocationRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChildLocationViewModel : ViewModel() {

    // ----------------------------
    // Firestore Real-time Listener
    // ----------------------------
    private var listener: ListenerRegistration? = null

    var zoneStates = mutableStateOf<Map<String, String>>(emptyMap())
        private set

    var timeInZone = mutableStateOf<Map<String, String>>(emptyMap())
        private set

    var lastAlert = mutableStateOf<String?>(null)
        private set

    fun startListening(childUID: String) {
        stopListening() // remove old listener if any

        val firestore = FirebaseFirestore.getInstance()
        listener = firestore.collection("child_position")
            .document(childUID)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChildLocationVM", "Listen failed: $error")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data ?: return@addSnapshotListener

                    val newZoneStates = data["zoneStates"] as? Map<String, String> ?: emptyMap()
                    val newTimeInZone = data["timeInZone"] as? Map<String, String> ?: emptyMap()

                    // Check for alerts
                    newZoneStates.forEach { (zoneId, state) ->
                        val prevState = zoneStates.value[zoneId]

                        if (prevState != state) {
                            when(state) {
                                "APPROACHING" -> lastAlert.value = "Child APPROACHING at zone $zoneId"
                                "INSIDE" -> lastAlert.value = "Child INSIDE at zone $zoneId"
                                "EXITED" -> lastAlert.value = "Child EXITED zone $zoneId"
                            }
                        }

                        // Check prolonged (>5 min)
                        if(state == "INSIDE") {
                            val duration = newTimeInZone[zoneId] ?: "00:00:00:00"
                            val parts = duration.split(":").map { it.toIntOrNull() ?: 0 }
                            val totalSeconds = parts[0]*86400 + parts[1]*3600 + parts[2]*60 + parts[3]
                            if(totalSeconds >= 300 && prevState != "PROLONGED") {
                                lastAlert.value = "Child PROLONGED at zone $zoneId"
                            }
                        }
                    }

                    zoneStates.value = newZoneStates
                    timeInZone.value = newTimeInZone
                }
            }
    }

    fun stopListening() {
        listener?.remove()
        listener = null
    }

    // ----------------------------
    // Network: Update Location to Backend
    // ----------------------------
    fun updateLocation(
        context: Context,
        parentId: String,
        childUID: String,
        lat: Double,
        lon: Double,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val request = UpdateLocationRequest(parentId, childUID, lat, lon)

        RetrofitClient.api.updateChildLocation(request)
            .enqueue(object : Callback<GenericResponse> {

                override fun onResponse(
                    call: Call<GenericResponse>,
                    response: Response<GenericResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Log.d("UPDATE_LOC", "Location updated successfully")
                        onComplete?.invoke(true)
                    } else {
                        Log.e(
                            "UPDATE_LOC",
                            "Error: ${response.code()} - ${response.body()?.message}"
                        )
                        Toast.makeText(context, "Failed to update location", Toast.LENGTH_SHORT).show()
                        onComplete?.invoke(false)
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Log.e("UPDATE_LOC", "Network error: ${t.message}")
                    Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                    onComplete?.invoke(false)
                }
            })
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
