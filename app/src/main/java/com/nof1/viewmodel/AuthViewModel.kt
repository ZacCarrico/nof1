package com.nof1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nof1.utils.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel for handling authentication operations.
 */
class AuthViewModel(
    private val authManager: AuthManager = AuthManager()
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        // Listen to authentication state changes
        viewModelScope.launch {
            authManager.authStateFlow().collect { user ->
                android.util.Log.d("AuthViewModel", "Auth state changed: user=${user?.uid ?: "null"}")
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = user != null
                )
            }
        }
    }
    
    /**
     * Sign in with email and password
     */
    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Email and password are required"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            val result = authManager.signInWithEmailPassword(email, password)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Sign in failed"
                )
            }
        }
    }
    
    /**
     * Sign up with email and password
     */
    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Email and password are required"
            )
            return
        }
        
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Password must be at least 6 characters"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            val result = authManager.createUserWithEmailPassword(email, password)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Account creation failed"
                )
            }
        }
    }
    
    /**
     * Sign in anonymously
     */
    fun signInAnonymously() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            val result = authManager.signInAnonymously()
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Anonymous sign in failed"
                )
            }
        }
    }
    
    /**
     * Sign out current user
     */
    fun signOut() {
        authManager.signOut()
        _uiState.value = _uiState.value.copy(
            isAuthenticated = false,
            errorMessage = null
        )
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Check if current user is anonymous
     */
    fun isAnonymousUser(): Boolean {
        return authManager.currentUser?.isAnonymous == true
    }
    
    /**
     * Get current user ID for debugging
     */
    fun getCurrentUserId(): String? {
        return authManager.currentUserId
    }
    
    /**
     * Link anonymous account with email/password (upgrade anonymous account)
     */
    fun linkWithEmailPassword(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Email and password are required"
            )
            return
        }
        
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Password must be at least 6 characters"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            try {
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
                val result = authManager.currentUser?.linkWithCredential(credential)?.await()
                
                if (result?.user != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to link account"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to link account"
                )
            }
        }
    }
}

/**
 * UI state for authentication screen
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null
) 