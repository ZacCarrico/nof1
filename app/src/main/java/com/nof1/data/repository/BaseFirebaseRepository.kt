package com.nof1.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Base Firebase repository providing common Firestore operations.
 * All Firebase repositories should extend this class.
 */
abstract class BaseFirebaseRepository {
    
    protected val firestore = FirebaseFirestore.getInstance()
    protected val auth = FirebaseAuth.getInstance()
    
    /**
     * Get current authenticated user ID
     */
    protected fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    /**
     * Require authenticated user, throw exception if not authenticated
     */
    protected fun requireUserId(): String {
        return getCurrentUserId() ?: throw IllegalStateException("User must be authenticated")
    }
    
    /**
     * Generic function to get documents from a collection as Flow
     */
    protected inline fun <reified T> getCollectionAsFlow(
        collectionRef: CollectionReference,
        crossinline query: (CollectionReference) -> Query = { it }
    ): Flow<List<T>> = flow {
        try {
            val snapshot = query(collectionRef).get().await()
            val items = snapshot.documents.mapNotNull { doc ->
                doc.toObject(T::class.java)
            }
            emit(items)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    /**
     * Generic function to get a document by ID
     */
    protected suspend inline fun <reified T> getDocumentById(
        collectionRef: CollectionReference,
        documentId: String
    ): T? {
        return try {
            val snapshot = collectionRef.document(documentId).get().await()
            snapshot.toObject(T::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generic function to add a document
     */
    protected suspend fun <T> addDocument(
        collectionRef: CollectionReference,
        data: T
    ): String? {
        return try {
            val docRef = collectionRef.add(data!!).await()
            docRef.id
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generic function to update a document
     */
    protected suspend fun <T> updateDocument(
        collectionRef: CollectionReference,
        documentId: String,
        data: T
    ): Boolean {
        return try {
            collectionRef.document(documentId).set(data!!).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Generic function to delete a document
     */
    protected suspend fun deleteDocument(
        collectionRef: CollectionReference,
        documentId: String
    ): Boolean {
        return try {
            collectionRef.document(documentId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
} 