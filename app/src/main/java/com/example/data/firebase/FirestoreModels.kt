package com.example.data.firebase

import com.google.firebase.Timestamp

data class FirestoreUserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class FirestoreKnowledgeSource(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val sourceType: String = "TEXT_NOTE",
    val isEnabled: Boolean = true,
    val summary: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class FirestoreChatSession(
    val sessionId: String = "",
    val userId: String = "",
    val title: String = "Nowa rozmowa",
    val createdAt: Timestamp? = null,
    val lastMessageAt: Timestamp? = null
)

data class FirestoreChatMessage(
    val id: String = "",
    val sessionId: String = "",
    val userId: String = "",
    val role: String = "user",
    val content: String = "",
    val isVoice: Boolean = false,
    val timestamp: Timestamp? = null
)
