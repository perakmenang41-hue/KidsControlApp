// File: app/src/main/java/com/example/kidscontrolapp/di/RepositoryModule.kt
package com.example.kidscontrolapp.di

import com.example.kidscontrolapp.ui.inbox.InboxApi
import com.example.kidscontrolapp.ui.inbox.InboxRepository
import com.example.kidscontrolapp.ui.inbox.RetrofitInboxRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Hilt module that provides:
 *   • a Retrofit instance
 *   • the InboxApi interface
 *   • a binding that maps RetrofitInboxRepository → InboxRepository
 *
 * The `@Binds` method must be inside an **abstract** module.
 * The `@Provides` methods live in the `companion object` so they can be static.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // -----------------------------------------------------------------
    // 1️⃣ Binds the concrete RetrofitInboxRepository to the
    //    abstract InboxRepository type.
    // -----------------------------------------------------------------
    @Binds
    @Singleton
    abstract fun bindInboxRepository(
        impl: RetrofitInboxRepository
    ): InboxRepository

    // -----------------------------------------------------------------
    // 2️⃣ Provide‑methods – placed in a companion object so they are
    //    static (the same as using an `object` module).
    // -----------------------------------------------------------------
    companion object {

        @Provides
        @Singleton
        fun provideRetrofit(): Retrofit = Retrofit.Builder()
            .baseUrl("https://kidscontrolapp.onrender.com/")   // <-- your backend base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        @Provides
        @Singleton
        fun provideInboxApi(retrofit: Retrofit): InboxApi =
            retrofit.create(InboxApi::class.java)
    }
}