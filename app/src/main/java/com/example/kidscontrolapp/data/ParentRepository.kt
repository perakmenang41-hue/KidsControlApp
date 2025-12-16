package com.example.kidscontrolapp.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Mirrors the exact shape of a document inside the
 *   collection  Parent_registered / <parentId>
 *
 * Only the fields you want the user to edit are listed as mutable.
 * The rest (fcmToken, password, …) are **not** touched by the update
 * method, so they stay safe.
 */
data class ParentInfo(
    val name: String = "",          // “Legal Name” – editable
    val country: String = "",       // editable
    val email: String = "",         // editable
    val phone: String = "",         // editable (your Firestore field is `phone`)
    val parentId: String = ""       // immutable – the same value you store as user‑ID
    // Any other fields (fcmToken, password, etc.) can stay in the document
    // but are simply not part of this data class.
)

class ParentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** -------------------- READ -------------------- */
    suspend fun getParentInfo(parentId: String): ParentInfo? = try {
        firestore.collection("Parent_registered")
            .document(parentId)
            .get()
            .await()
            .toObject(ParentInfo::class.java)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    /** -------------------- UPDATE --------------------
     *  Writes only the mutable fields. The immutable `parentId`
     *  is never sent, so it can’t be overwritten.
     */
    suspend fun updateParentInfo(parentId: String, updated: ParentInfo): Boolean = try {
        val updates = mapOf(
            "name"    to updated.name,
            "country" to updated.country,
            "email"   to updated.email,
            "phone"   to updated.phone
            // NOTE: we deliberately omit fcmToken, password, etc.
        )
        firestore.collection("Parent_registered")
            .document(parentId)
            .update(updates)
            .await()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}