package com.ultron.assistant.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val conversationId: String,
    val sender: MessageSender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val imageBase64: String? = null,
    val actionType: String? = null,
    val actionStatus: String? = null
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}

enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}
