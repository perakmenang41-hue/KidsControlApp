package com.example.kidscontrolapp.utils

import com.google.firebase.firestore.FirebaseFirestore

object FirestoreProvider {

    // Always use real Firestore
    fun getFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
}
