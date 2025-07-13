package com.nof1.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose

/**
 * Authentication manager for Firebase Auth operations.
 * Handles user authentication, registration, and session management.
 */
class AuthManager {
    
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Get current authenticated user
     */
    val currentUser: FirebaseUser?
        get() = auth.currentUser
    
    /**
     * Check if user is authenticated
     */
    val isAuthenticated: Boolean
        get() = currentUser != null
    
    /**
     * Get current user ID
     */
    val currentUserId: String?
        get() = currentUser?.uid
    
    /**
     * Listen to authentication state changes
     */
    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        android.util.Log.d("AuthManager", "Setting up authentication state listener")
        
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            android.util.Log.d("AuthManager", "Auth state changed: user=${user?.uid ?: "null"}")
            trySend(user).isSuccess
        }
        
        auth.addAuthStateListener(listener)
        
        // Emit current state immediately
        trySend(currentUser).isSuccess
        
        awaitClose {
            android.util.Log.d("AuthManager", "Removing authentication state listener")
            auth.removeAuthStateListener(listener)
        }
    }
    
    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmailPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Sign in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create account with email and password
     */
    suspend fun createUserWithEmailPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Account creation failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign in anonymously (for trial/demo usage)
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Anonymous sign in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
    }
    
    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete current user account
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateUserProfile(displayName: String? = null, photoUrl: String? = null): Result<Unit> {
        return try {
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .apply {
                    displayName?.let { setDisplayName(it) }
                    photoUrl?.let { setPhotoUri(android.net.Uri.parse(it)) }
                }
                .build()
            
            currentUser?.updateProfile(profileUpdates)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 