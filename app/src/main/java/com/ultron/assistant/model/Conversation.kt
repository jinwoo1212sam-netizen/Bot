package com.ultron.assistant.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Conversation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "New Conversation",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
            return sdf.format(Date(updatedAt))
        }
}
