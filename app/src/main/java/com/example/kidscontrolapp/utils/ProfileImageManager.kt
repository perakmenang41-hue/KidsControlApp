package com.example.kidscontrolapp.utils

import android.content.Context
import android.net.Uri

object ProfileImageManager {

    private const val PREF_NAME = "profile_prefs"
    private const val KEY_IMAGE_URI = "profile_image_uri"

    fun saveProfileImage(context: Context, uri: Uri?) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_IMAGE_URI, uri?.toString()).apply()
    }

    fun loadProfileImage(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val uriString = prefs.getString(KEY_IMAGE_URI, null)
        return uriString?.let { Uri.parse(it) }
    }
}
