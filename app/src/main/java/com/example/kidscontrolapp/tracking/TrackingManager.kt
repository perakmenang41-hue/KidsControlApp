package com.example.kidscontrolapp.tracking

import android.content.Context
import com.example.kidscontrolapp.viewmodel.ChildLocationViewModel
import kotlinx.coroutines.*

object TrackingManager {

    private var job: Job? = null

    fun startTracking(
        viewModel: ChildLocationViewModel,
        parentId: String,
        childUID: String
    ) {
        if (job != null) return

        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                viewModel.sendLocationToBackend(parentId, childUID)
                delay(5000) // every 5 seconds
            }
        }
    }

    fun stopTracking() {
        job?.cancel()
        job = null
    }

    fun isTracking(): Boolean = job != null
}

