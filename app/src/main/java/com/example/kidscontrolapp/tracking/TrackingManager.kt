package com.example.kidscontrolapp.tracking

import kotlinx.coroutines.*

object TrackingManager {

    private var job: Job? = null

    /**
     * Starts a coroutine that calls `sendLocationToBackend` every 5 seconds.
     *
     * @param handler  Any implementation of `LocationHandler`
     *                 (UI ViewModel or Service helper).
     */
    fun startTracking(
        handler: LocationHandler,
        parentId: String,
        childUID: String
    ) {
        if (job != null) return

        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                handler.sendLocationToBackend(parentId, childUID)
                delay(5_000)   // 5 seconds
            }
        }
    }

    fun stopTracking() {
        job?.cancel()
        job = null
    }

    fun isTracking(): Boolean = job != null
}