// File: app/src/main/java/com/example/kidscontrolapp/data/InboxDao.kt
package com.example.kidscontrolapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InboxDao {

    @Query("SELECT * FROM inbox ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<InboxMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: InboxMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<InboxMessage>)

    @Query("DELETE FROM inbox")
    suspend fun clearAll()
}