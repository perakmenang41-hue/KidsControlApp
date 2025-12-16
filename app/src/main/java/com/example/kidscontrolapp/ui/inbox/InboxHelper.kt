package com.example.kidscontrolapp.ui.inbox

import javax.inject.Inject
import android.util.Log
import com.example.kidscontrolapp.data.InboxMessageResponse
import com.example.kidscontrolapp.data.InboxMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Plain injectable class (NOT a ViewModel). Holds the same business
 * logic that the UI ViewModel uses, but can be used from a Service.
 */
class InboxHelper @Inject constructor(
    private val repository: InboxRepository
) {

    // -----------------------------------------------------------------
    // Coroutine‑friendly version – you can call this from a Service
    // coroutine scope if you prefer.
    // -----------------------------------------------------------------
    suspend fun fetchInboxMessages(): List<InboxMessage>? {
        return try {
            val response = repository.getInbox()
            response.messages
        } catch (e: Exception) {
            Log.e("InboxHelper", "Failed to fetch inbox (suspend)", e)
            null
        }
    }

    // -----------------------------------------------------------------
    // Callback‑based version – perfect for a plain Service.
    // -----------------------------------------------------------------
    fun fetchInboxMessages(
        onResult: (messages: List<InboxMessage>?, error: Throwable?) -> Unit
    ) {
        repository.getInboxAsync().enqueue(object : Callback<InboxMessageResponse> {
            override fun onResponse(
                call: Call<InboxMessageResponse>,
                response: Response<InboxMessageResponse>
            ) {
                if (response.isSuccessful) {
                    onResult(response.body()?.messages, null)
                } else {
                    onResult(null, Exception("HTTP ${response.code()}"))
                }
            }

            override fun onFailure(call: Call<InboxMessageResponse>, t: Throwable) {
                onResult(null, t)
            }
        })
    }

    // -----------------------------------------------------------------
    // Utility used by the service – counts unread messages.
    // -----------------------------------------------------------------
    fun countUnread(messages: List<InboxMessage>?): Int =
        messages?.count { !it.isRead } ?: 0

    // -----------------------------------------------------------------
    // If you still need to **store** a remote message in Room,
    // expose a thin wrapper that forwards to your existing DAO.
    // (Replace `InboxDao` with the actual DAO you use.)
    // -----------------------------------------------------------------
    fun storeRemoteMessage(message: InboxMessage) {
        // Example – you probably already have a DAO for this:
        // inboxDao.insert(message)
        // For now we just log it:
        Log.d("InboxHelper", "Storing message: $message")
    }
}