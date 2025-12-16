// File: app/src/main/java/com/example/kidscontrolapp/data/InboxRepository.kt
package com.example.kidscontrolapp.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Simple repository that the ViewModel will use.
 * It exposes a Flow of InboxMessage (the Room entity) that already
 * contains childName, status, zoneName, and readableTime.
 */
class InboxRepository @Inject constructor(
    private val inboxDao: InboxDao
) {
    /** Public stream of all inbox messages. */
    val messages: Flow<List<InboxMessage>> = inboxDao.getAllMessages()

    /** Insert a new message – can be called from a Service/Helper. */
    suspend fun insert(message: InboxMessage) = inboxDao.insert(message)

    /** Bulk insert – useful after a network sync. */
    suspend fun insertAll(messages: List<InboxMessage>) = inboxDao.insertAll(messages)

    /** Clear all messages – optional utility. */
    suspend fun clearAll() = inboxDao.clearAll()
}