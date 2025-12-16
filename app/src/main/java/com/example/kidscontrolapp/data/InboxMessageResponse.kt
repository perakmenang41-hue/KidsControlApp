// File: app/src/main/java/com/example/kidscontrolapp/network/InboxMessageResponse.kt
package com.example.kidscontrolapp.data

/**
 * The JSON payload returned by the `/inbox` endpoint.
 * It must contain a `messages` array.
 */
data class InboxMessageResponse(
    val messages: List<InboxMessage>
)