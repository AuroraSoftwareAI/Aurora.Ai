package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class SourceType {
    TEXT_NOTE,
    DOCUMENT,
    URL_REFERENCE,
    USER_PROFILE,
    CUSTOM_RULE
}

@Entity(tableName = "knowledge_sources")
data class KnowledgeSource(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val sourceType: SourceType = SourceType.TEXT_NOTE,
    val isEnabled: Boolean = true,
    val summary: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class FactCategory {
    STYLE,
    VOCABULARY,
    HUMOR,
    PREFERENCE,
    GENERAL
}

@Entity(tableName = "learned_facts")
data class LearnedFact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fact: String,
    val category: FactCategory = FactCategory.GENERAL,
    val isActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey
    val sessionId: String = UUID.randomUUID().toString(),
    val title: String = "Nowa rozmowa",
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoice: Boolean = false
)
