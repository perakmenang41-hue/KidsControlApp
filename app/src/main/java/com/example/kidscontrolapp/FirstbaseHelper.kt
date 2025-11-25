package com.example.kidscontrolapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseHelper {
    fun auth() = FirebaseAuth.getInstance()

    // root ref to kids_location for a parent
    fun kidsLocationRefForParent(parentUid: String): DatabaseReference {
        return FirebaseDatabase.getInstance(
            "https://kidscontrolfyp-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference("Kids_Location").child(parentUid)
    }
}
