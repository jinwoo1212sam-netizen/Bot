package com.ultron.assistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "USER" or "ULTRON"
    val message: String,
    val actionType: String? = null,
    val actionStatus: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
