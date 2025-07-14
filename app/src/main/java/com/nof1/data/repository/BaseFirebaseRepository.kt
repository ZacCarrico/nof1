package com.nof1.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose

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
     * Generic function to get documents from a collection as Flow with real-time updates
     */
    protected inline fun <reified T> getCollectionAsFlow(
        collectionRef: CollectionReference,
        crossinline query: (CollectionReference) -> Query = { it }
    ): Flow<List<T>> = callbackFlow {
        val currentUserId = getCurrentUserId()
        android.util.Log.d("BaseFirebaseRepository", "Setting up real-time listener for ${collectionRef.path} with userId: $currentUserId")
        
        val finalQuery = query(collectionRef)
        android.util.Log.d("BaseFirebaseRepository", "Query path: ${finalQuery}")
        
        val listener = finalQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("BaseFirebaseRepository", "Error in real-time listener for ${collectionRef.path}: ${error.message}", error)
                // Don't close the flow on error, Firebase will retry
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                android.util.Log.d("BaseFirebaseRepository", "Received ${snapshot.documents.size} documents from ${collectionRef.path}")
                android.util.Log.d("BaseFirebaseRepository", "Snapshot metadata: fromCache=${snapshot.metadata.isFromCache}")
                
                snapshot.documents.forEach { doc ->
                    android.util.Log.v("BaseFirebaseRepository", "Document ${doc.id}: ${doc.data}")
                }
                
                val items = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(T::class.java)?.also {
                            android.util.Log.v("BaseFirebaseRepository", "Successfully parsed document ${doc.id}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("BaseFirebaseRepository", "Failed to parse document ${doc.id}: ${e.message}")
                        null
                    }
                }
                android.util.Log.d("BaseFirebaseRepository", "Emitting ${items.size} items from ${collectionRef.path}")
                trySend(items).isSuccess
            } else {
                android.util.Log.w("BaseFirebaseRepository", "Received null snapshot for ${collectionRef.path}")
                trySend(emptyList()).isSuccess
            }
        }
        
        awaitClose {
            android.util.Log.d("BaseFirebaseRepository", "Removing real-time listener for ${collectionRef.path}")
            listener.remove()
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
            android.util.Log.d("BaseFirebaseRepository", "Attempting to add document to ${collectionRef.path}")
            android.util.Log.d("BaseFirebaseRepository", "Document data: $data")
            android.util.Log.d("BaseFirebaseRepository", "Current user: ${auth.currentUser?.uid}")
            android.util.Log.d("BaseFirebaseRepository", "Firebase project ID: ${firestore.app.options.projectId}")
            val docRef = collectionRef.add(data!!).await()
            android.util.Log.d("BaseFirebaseRepository", "Successfully created document with ID: ${docRef.id}")
            docRef.id
        } catch (e: Exception) {
            android.util.Log.e("BaseFirebaseRepository", "Failed to add document to ${collectionRef.path}: ${e.message}", e)
            android.util.Log.e("BaseFirebaseRepository", "Exception type: ${e.javaClass.simpleName}")
            if (e.cause != null) {
                android.util.Log.e("BaseFirebaseRepository", "Root cause: ${e.cause?.javaClass?.simpleName}: ${e.cause?.message}")
            }
            // Additional debugging for common issues
            when (e) {
                is com.google.firebase.firestore.FirebaseFirestoreException -> {
                    android.util.Log.e("BaseFirebaseRepository", "Firestore error code: ${e.code}")
                    android.util.Log.e("BaseFirebaseRepository", "Firestore error message: ${e.localizedMessage}")
                }
                is com.google.firebase.auth.FirebaseAuthException -> {
                    android.util.Log.e("BaseFirebaseRepository", "Auth error code: ${e.errorCode}")
                }
                is SecurityException -> {
                    android.util.Log.e("BaseFirebaseRepository", "Security error - check Firestore rules")
                }
            }
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