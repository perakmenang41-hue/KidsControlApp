package com.example.kidscontrolapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [InboxMessage::class], version = 1, exportSchema = false)
abstract class KidsControlDatabase : RoomDatabase() {

    abstract fun inboxDao(): InboxDao

    companion object {
        @Volatile private var INSTANCE: KidsControlDatabase? = null

        fun getInstance(context: Context): KidsControlDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    KidsControlDatabase::class.java,
                    "kids_control.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}