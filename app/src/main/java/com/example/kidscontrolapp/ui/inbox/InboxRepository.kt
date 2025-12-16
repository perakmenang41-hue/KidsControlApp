package com.example.kidscontrolapp.ui.inbox

import com.example.kidscontrolapp.data.InboxMessageResponse
import retrofit2.Call
import retrofit2.http.GET
import javax.inject.Inject

/**
 * Repository interface – used by both the UI ViewModel and the Service‑side helper.
 */
interface InboxRepository {

    /** Suspend version – used by the ViewModel (coroutines) */
    suspend fun getInbox(): InboxMessageResponse

    /** Callback‑based version – convenient for a Service that doesn’t use coroutines */
    fun getInboxAsync(): Call<InboxMessageResponse>
}

/**
 * Retrofit implementation of the repository.
 * Replace the base URL / endpoint with your actual backend.
 */
class RetrofitInboxRepository @Inject constructor(
    private val api: InboxApi
) : InboxRepository {

    override suspend fun getInbox(): InboxMessageResponse = api.getInbox()

    override fun getInboxAsync(): Call<InboxMessageResponse> = api.getInboxAsync()
}

/**
 * Retrofit API definition.
 */
interface InboxApi {
    @GET("inbox")
    suspend fun getInbox(): InboxMessageResponse

    @GET("inbox")
    fun getInboxAsync(): Call<InboxMessageResponse>
}