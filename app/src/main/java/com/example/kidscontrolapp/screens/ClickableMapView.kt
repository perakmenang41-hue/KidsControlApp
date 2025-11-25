package com.example.kidscontrolapp.screens

import android.content.Context
import android.util.AttributeSet
import org.osmdroid.views.MapView

// ✅ renamed to avoid redeclaration
class AccessibleMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : MapView(context, attrs) {

    override fun performClick(): Boolean {
        return super.performClick()
    }
}
