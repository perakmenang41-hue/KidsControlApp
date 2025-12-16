// File: app/src/main/java/com/example/kidscontrolapp/di/DatabaseModule.kt
package com.example.kidscontrolapp.di

import android.content.Context
import androidx.room.Room
import com.example.kidscontrolapp.data.InboxDao
import com.example.kidscontrolapp.data.KidsControlDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): KidsControlDatabase =
        Room.databaseBuilder(
            ctx,
            KidsControlDatabase::class.java,
            "kids_control.db"
        ).fallbackToDestructiveMigration()   // optional – adjust for migrations
            .build()

    @Provides @Singleton
    fun provideInboxDao(db: KidsControlDatabase): InboxDao = db.inboxDao()
}