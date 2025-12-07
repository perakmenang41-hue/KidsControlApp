package com.example.kidscontrolapp.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // ⚠️ YOUR LOCAL BACKEND IP
    private const val BASE_URL = "http://192.168.0.6:5000/"

    // ⭐ MUST MATCH POSTMAN TOKEN
    private const val API_KEY = "supersecretapikey"

    // Logging interceptor
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttp client with Authorization header and logging
    private val client = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(newRequest)
        })
        .addInterceptor(logging)  // ⭐ Logging added
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)  // ⭐ Use client with Authorization header + logging
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
