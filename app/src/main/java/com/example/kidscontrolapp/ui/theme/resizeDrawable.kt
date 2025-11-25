package com.example.kidscontrolapp.ui.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import android.content.Context


// Helper function to resize drawable
fun resizeDrawable(context: Context, drawableId: Int, width: Int, height: Int): Drawable {
    val drawable = ContextCompat.getDrawable(context, drawableId)!!
    val bitmap = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return BitmapDrawable(context.resources, bitmap)
}
