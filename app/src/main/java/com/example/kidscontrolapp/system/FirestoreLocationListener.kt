package com.example.kidscontrolapp.system

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.kidscontrolapp.utils.FirestoreProvider

object FirestoreLocationListener {

    private var listener: ListenerRegistration? = null
    private val firestore = FirestoreProvider.getFirestore()

    fun start(childUid: String, onUpdate: (Double, Double) -> Unit) {
        listener = firestore.collection("child_locations")
            .document(childUid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val lat = snapshot.getDouble("latitude") ?: 0.0
                    val lng = snapshot.getDouble("longitude") ?: 0.0
                    onUpdate(lat, lng)
                }
            }
    }

    fun stop() {
        listener?.remove()
    }
}
