package com.example.kidscontrolapp.network

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.HTTP

interface ApiService {

    // ------------------------
    // Add Danger Zone
    // ------------------------
    @Headers("Content-Type: application/json")
    @POST("api/danger-zone/add")
    fun addDangerZone(
        @Body request: DangerZoneRequest
    ): Call<DangerZoneResponse>

    // ------------------------
    // Remove Danger Zone
    // ------------------------
    @Headers("Content-Type: application/json")
    @HTTP(method = "DELETE", path = "api/danger-zone/delete/{zoneId}", hasBody = true)
    fun removeDangerZone(
        @Path("zoneId") zoneId: String,
        @Body body: DeleteZoneRequest
    ): Call<DangerZoneResponse>

    // ------------------------
    // Update Child Location (Clean Approach)
    // ------------------------
    @Headers("Content-Type: application/json")
    @POST("api/child/family/update-location")
    fun updateChildLocation(
        @Body request: UpdateLocationRequest
    ): Call<GenericResponse>

    // ------------------------
    // Companion object for Retrofit instance
    // ------------------------
    companion object {
        private const val BASE_URL = "https://kidscontrolapp.onrender.com/" // your backend URL

        fun create(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ApiService::class.java)
        }
    }
}
