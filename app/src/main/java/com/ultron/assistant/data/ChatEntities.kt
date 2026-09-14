package com.ultron.assistant.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ultron.assistant.model.Conversation
import com.ultron.assistant.model.Message
import com.ultron.assistant.model.MessageSender

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Conversation = Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["conversationId"])]
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val sender: String, // "USER" or "ASSISTANT" or "SYSTEM"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val imageBase64: String? = null,
    val actionType: String? = null,
    val actionStatus: String? = null
) {
    fun toDomain(): Message = Message(
        id = id,
        conversationId = conversationId,
        sender = when (sender.uppercase()) {
            "USER" -> MessageSender.USER
            "SYSTEM" -> MessageSender.SYSTEM
            else -> MessageSender.ASSISTANT
        },
        content = content,
        timestamp = timestamp,
        imageUri = imageUri,
        imageBase64 = imageBase64,
        actionType = actionType,
        actionStatus = actionStatus
    )
}
