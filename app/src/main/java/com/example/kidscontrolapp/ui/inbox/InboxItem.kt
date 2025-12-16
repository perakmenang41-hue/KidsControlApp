package com.example.kidscontrolapp.ui.inbox

import com.example.kidscontrolapp.data.InboxMessage

/**
 * UI‑only representation of an inbox entry.
 *
 * This class mirrors the columns that really exist in the Room table
 * (childName, status, zoneName, timestamp, isRead).  If you later add
 * extra columns (e.g. title, body) to the entity you can extend this
 * class accordingly.
 *
 * Keeping a separate UI model is useful when you want to add UI‑specific
 * derived values (e.g. formatted strings) without polluting the database
 * entity.
 */
data class InboxItem(
    val id: Long,               // matches the PK type in InboxMessage
    val childName: String,
    val status: String,
    val zoneName: String,
    val timestamp: Long,
    val isRead: Boolean,
    /** A copy of the entity’s human‑readable time string (optional). */
    val readableTime: String = ""
) {
    companion object {
        /**
         * Convert a Room `InboxMessage` into an `InboxItem`.
         *
         * If you ever add new fields to `InboxMessage` (e.g. title/body),
         * just pull them into the constructor here.
         */
        fun fromEntity(msg: InboxMessage): InboxItem = InboxItem(
            id = msg.id,
            childName = msg.childName,
            status = msg.status,
            zoneName = msg.zoneName,
            timestamp = msg.timestamp,
            isRead = msg.isRead,
            readableTime = msg.readableTime          // reuse the entity helper
        )
    }
}