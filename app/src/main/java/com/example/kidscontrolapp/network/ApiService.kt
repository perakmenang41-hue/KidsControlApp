package com.example.kidscontrolapp.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("danger-zone/add")
    fun addDangerZone(@Body request: DangerZoneRequest): Call<DangerZoneResponse>
}
