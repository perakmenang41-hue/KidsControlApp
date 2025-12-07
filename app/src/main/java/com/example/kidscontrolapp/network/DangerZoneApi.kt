package com.example.kidscontrolapp.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface DangerZoneApi {

    @POST("dangerZones/add") // replace with your actual backend path
    fun addDangerZone(
        @Body request: DangerZoneRequest
    ): Call<DangerZoneResponse>

    // Example for future fetching:
    // @GET("dangerZones/{parentId}")
    // fun getDangerZones(@Path("parentId") parentId: String): Call<List<DangerZoneResponse>>
}
