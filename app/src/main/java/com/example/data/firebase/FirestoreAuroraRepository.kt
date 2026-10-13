package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreAuroraRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun saveUserProfile(displayName: String, email: String) {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "displayName" to displayName,
            "email" to email,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            userDoc.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDoc.path)
            throw e
        }
    }

    fun observeKnowledgeSources(): Flow<List<FirestoreKnowledgeSource>> = callbackFlow {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("knowledgeSources")
        val registration = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, collectionRef.path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.toObjects(
                    FirestoreKnowledgeSource::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                trySend(items)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveKnowledgeSource(source: FirestoreKnowledgeSource) {
        val uid = requireUserId()
        val docId = if (source.id.isNotBlank()) source.id else db.collection("users").document(uid).collection("knowledgeSources").document().id
        val docRef = db.collection("users").document(uid).collection("knowledgeSources").document(docId)
        val payload = mapOf(
            "id" to docId,
            "userId" to uid,
            "title" to source.title,
            "content" to source.content,
            "sourceType" to source.sourceType,
            "isEnabled" to source.isEnabled,
            "summary" to source.summary,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteKnowledgeSource(sourceId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("knowledgeSources").document(sourceId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeChatSessions(): Flow<List<FirestoreChatSession>> = callbackFlow {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("chatSessions")
        val registration = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, collectionRef.path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val sessions = snapshot.toObjects(
                    FirestoreChatSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                trySend(sessions)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveChatSession(session: FirestoreChatSession) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("chatSessions").document(session.sessionId)
        val payload = mapOf(
            "sessionId" to session.sessionId,
            "userId" to uid,
            "title" to session.title,
            "lastMessageAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeMessages(sessionId: String): Flow<List<FirestoreChatMessage>> = callbackFlow {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("chatSessions").document(sessionId).collection("messages")
        val registration = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, collectionRef.path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val messages = snapshot.toObjects(
                    FirestoreChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                trySend(messages)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun addMessage(message: FirestoreChatMessage) {
        val uid = requireUserId()
        val docId = if (message.id.isNotBlank()) message.id else db.collection("users").document(uid).collection("chatSessions").document(message.sessionId).collection("messages").document().id
        val docRef = db.collection("users").document(uid).collection("chatSessions").document(message.sessionId).collection("messages").document(docId)
        val payload = mapOf(
            "id" to docId,
            "sessionId" to message.sessionId,
            "userId" to uid,
            "role" to message.role,
            "content" to message.content,
            "isVoice" to message.isVoice,
            "timestamp" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }
}
