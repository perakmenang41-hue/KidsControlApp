// File: app/src/main/java/com/example/kidscontrolapp/data/InboxMessage.kt
package com.example.kidscontrolapp.data

import android.text.format.DateUtils
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inbox")
data class InboxMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childName: String,
    val status: String,               // INSIDE, OUTSIDE, APPROACHING, PROLONGED, EXITED
    val zoneName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false       // <-- added – used by countUnread()
) {
    // UI‑friendly relative time string
    val readableTime: String
        get() = DateUtils.getRelativeTimeSpanString(
            timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
}