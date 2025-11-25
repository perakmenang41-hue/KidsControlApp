package com.example.kidscontrolapp.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.database.*

class TrackerViewModel : ViewModel() {
    val latitude = mutableStateOf<Double?>(null)
    val longitude = mutableStateOf<Double?>(null)
    private var listener: ValueEventListener? = null

    fun startListening(childUID: String) {
        val ref = FirebaseDatabase.getInstance().getReference("Kids_Location").child(childUID)
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                latitude.value = snapshot.child("latitude").getValue(Double::class.java)
                longitude.value = snapshot.child("longitude").getValue(Double::class.java)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener!!)
    }

    fun stopListening(childUID: String) {
        val ref = FirebaseDatabase.getInstance().getReference("Kids_Location").child(childUID)
        listener?.let { ref.removeEventListener(it) }
    }
}
