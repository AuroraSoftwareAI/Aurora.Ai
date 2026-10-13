package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firebase.FirestoreAuroraRepository
import com.example.data.firebase.FirestoreChatMessage
import com.example.data.firebase.FirestoreChatSession
import com.example.data.firebase.FirestoreKnowledgeSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuroraRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun authenticatedUser_canSaveAndObserveKnowledgeSource() = runBlocking {
        val uid = signInTestUser("aurora_tester@example.com")
        val repository = FirestoreAuroraRepository(firestore, auth)

        val source = FirestoreKnowledgeSource(
            id = "test_src_1",
            userId = uid,
            title = "Testowa wiedza Aurora",
            content = "Aurora uczy się stylu użytkownika",
            sourceType = "TEXT_NOTE",
            isEnabled = true
        )

        repository.saveKnowledgeSource(source)

        val list = repository.observeKnowledgeSources().first()
        assertTrue(list.any { it.id == "test_src_1" && it.title == "Testowa wiedza Aurora" })
    }

    @Test
    fun authenticatedUser_canSaveAndObserveChatSessionAndMessage() = runBlocking {
        val uid = signInTestUser("aurora_chat_tester@example.com")
        val repository = FirestoreAuroraRepository(firestore, auth)

        val session = FirestoreChatSession(
            sessionId = "session_rule_1",
            userId = uid,
            title = "Rozmowa testowa"
        )
        repository.saveChatSession(session)

        val message = FirestoreChatMessage(
            id = "msg_rule_1",
            sessionId = "session_rule_1",
            userId = uid,
            role = "user",
            content = "Cześć Aurora!"
        )
        repository.addMessage(message)

        val sessions = repository.observeChatSessions().first()
        assertTrue(sessions.any { it.sessionId == "session_rule_1" })

        val messages = repository.observeMessages("session_rule_1").first()
        assertTrue(messages.any { it.id == "msg_rule_1" && it.content == "Cześć Aurora!" })
    }

    @Test(expected = Exception::class)
    fun unauthenticatedUser_cannotAccessRepository(): Unit = runBlocking {
        auth.signOut()
        val repository = FirestoreAuroraRepository(firestore, auth)
        repository.requireUserId()
        Unit
    }
}
