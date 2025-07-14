package com.nof1

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.nof1.utils.NotificationHelper
import com.nof1.utils.AuthManager
import com.nof1.utils.SecureStorage
import com.nof1.data.repository.*

/**
 * Application class for the Nof1 app.
 * Now uses Firebase-only repositories.
 */
class Nof1Application : Application() {
    
    // Authentication
    val authManager by lazy { AuthManager() }
    
    // Secure Storage
    val secureStorage by lazy { SecureStorage(this) }
    
    // Firebase-only Repositories
    val projectRepository by lazy { ProjectRepository() }
    val hypothesisRepository by lazy { HypothesisRepository() }
    val experimentRepository by lazy { ExperimentRepository() }
    val logEntryRepository by lazy { LogEntryRepository() }
    val noteRepository by lazy { NoteRepository() }
    val reminderRepository by lazy { ReminderRepository() }
    val hypothesisGenerationRepository by lazy { HypothesisGenerationRepository(secureStorage, hypothesisRepository) }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Configure Firestore for debug and offline support
        configureFirestore()
        
        // Initialize WorkManager
        val configuration = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
        WorkManager.initialize(this, configuration)
        
        NotificationHelper.createNotificationChannel(this)
    }
    
    private fun configureFirestore() {
        val firestore = FirebaseFirestore.getInstance()
        
        // Enable Firestore debug logging for WiFi debugging issues
        FirebaseFirestore.setLoggingEnabled(true)
        
        // Enable offline persistence for reliable data access during network issues
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        firestore.firestoreSettings = settings
        
        android.util.Log.d("Nof1Application", "Firebase Firestore configured with offline persistence enabled")
    }
} 