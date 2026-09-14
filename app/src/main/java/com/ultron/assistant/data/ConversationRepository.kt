package com.ultron.assistant.data

import com.ultron.assistant.model.Conversation
import com.ultron.assistant.model.Message
import com.ultron.assistant.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class ConversationRepository(
    private val conversationDao: ConversationDao
) {

    val conversations: Flow<List<Conversation>> =
        conversationDao.getAllConversations().map { list ->
            list.map { it.toDomain() }
        }

    fun getMessages(conversationId: String): Flow<List<Message>> =
        conversationDao.getMessagesForConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getRecentMessages(conversationId: String, limit: Int): List<Message> =
        withContext(Dispatchers.IO) {
            conversationDao.getRecentMessages(conversationId, limit).reversed().map { it.toDomain() }
        }

    suspend fun createConversation(title: String = "New Conversation"): Conversation =
        withContext(Dispatchers.IO) {
            val conv = Conversation(
                id = UUID.randomUUID().toString(),
                title = title,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            conversationDao.insertConversation(
                ChatSessionEntity(
                    id = conv.id,
                    title = conv.title,
                    createdAt = conv.createdAt,
                    updatedAt = conv.updatedAt
                )
            )
            conv
        }

    suspend fun renameConversation(conversationId: String, newTitle: String) =
        withContext(Dispatchers.IO) {
            conversationDao.updateConversationTitle(
                id = conversationId,
                title = newTitle.trim().ifEmpty { "Conversation" },
                updatedAt = System.currentTimeMillis()
            )
        }

    suspend fun deleteConversation(conversationId: String) =
        withContext(Dispatchers.IO) {
            conversationDao.clearMessagesForConversation(conversationId)
            conversationDao.deleteConversation(conversationId)
        }

    suspend fun clearConversationMessages(conversationId: String) =
        withContext(Dispatchers.IO) {
            conversationDao.clearMessagesForConversation(conversationId)
            conversationDao.touchConversation(conversationId)
        }

    suspend fun deleteAllConversations() =
        withContext(Dispatchers.IO) {
            conversationDao.deleteAllMessages()
            conversationDao.deleteAllConversations()
        }

    suspend fun addMessage(message: Message) =
        withContext(Dispatchers.IO) {
            // Ensure session exists or touch timestamp
            val session = conversationDao.getConversationById(message.conversationId)
            if (session == null) {
                val title = if (message.sender == MessageSender.USER) {
                    message.content.take(30).trim().ifEmpty { "Conversation" }
                } else {
                    "Conversation"
                }
                conversationDao.insertConversation(
                    ChatSessionEntity(
                        id = message.conversationId,
                        title = title,
                        createdAt = message.timestamp,
                        updatedAt = message.timestamp
                    )
                )
            } else {
                // If the session still has default title and this is user message, update title
                if (session.title == "New Conversation" && message.sender == MessageSender.USER && message.content.isNotBlank()) {
                    conversationDao.updateConversationTitle(
                        id = message.conversationId,
                        title = message.content.take(32).trim(),
                        updatedAt = message.timestamp
                    )
                } else {
                    conversationDao.touchConversation(message.conversationId, message.timestamp)
                }
            }

            conversationDao.insertChatMessage(
                ChatMessageEntity(
                    id = message.id,
                    conversationId = message.conversationId,
                    sender = message.sender.name,
                    content = message.content,
                    timestamp = message.timestamp,
                    imageUri = message.imageUri,
                    imageBase64 = message.imageBase64,
                    actionType = message.actionType,
                    actionStatus = message.actionStatus
                )
            )
        }

    suspend fun getOrCreateLatestConversation(): Conversation =
        withContext(Dispatchers.IO) {
            val all = conversationDao.getRecentLegacyMessages()
            // Check if any chat sessions exist
            val entity = conversationDao.getConversationById("default_session")
            if (entity != null) {
                entity.toDomain()
            } else {
                val newConv = Conversation(
                    id = "default_session",
                    title = "Main Conversation",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                conversationDao.insertConversation(
                    ChatSessionEntity(
                        id = newConv.id,
                        title = newConv.title,
                        createdAt = newConv.createdAt,
                        updatedAt = newConv.updatedAt
                    )
                )
                newConv
            }
        }
}
