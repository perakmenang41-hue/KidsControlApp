package com.example.kidscontrolapp.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class DangerZoneAlertViewModel : ViewModel() {

    var zoneStatus = mutableStateOf("OUTSIDE")
        private set

    fun startListening(parentId: String, childId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("Parent_registered")
            .document(parentId)
            .collection("child_registered")
            .document(childId)
            .collection("dangerZoneStatus")
            .document("current")
            .addSnapshotListener { snapshot, _ ->

                val status = snapshot?.getString("status") ?: "OUTSIDE"
                zoneStatus.value = status
            }
    }
}
