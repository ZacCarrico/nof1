package com.nof1

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.nof1.data.local.Nof1Database
import com.nof1.utils.NotificationHelper
import com.nof1.utils.AuthManager
import com.nof1.data.repository.*

/**
 * Application class for the Nof1 app.
 */
class Nof1Application : Application() {
    val database by lazy { Nof1Database.getDatabase(this) }
    
    // Authentication
    val authManager by lazy { AuthManager() }
    
    // Firebase Repositories
    val firebaseProjectRepository by lazy { FirebaseProjectRepository() }
    val firebaseHypothesisRepository by lazy { FirebaseHypothesisRepository() }
    val firebaseExperimentRepository by lazy { FirebaseExperimentRepository() }
    val firebaseLogEntryRepository by lazy { FirebaseLogEntryRepository() }
    
    // Firebase Mapping Repository
    val firebaseMappingRepository by lazy { 
        FirebaseMappingRepository(database.firebaseMappingDao(), authManager)
    }
    
    // Hybrid Repositories (combining local + cloud)  
    val hybridHypothesisRepository by lazy {
        HybridHypothesisRepository(database.hypothesisDao(), firebaseHypothesisRepository, firebaseMappingRepository)
    }
    val hybridProjectRepository by lazy { 
        val projectRepo = HybridProjectRepository(database.projectDao(), firebaseProjectRepository, firebaseMappingRepository)
        // Set hypothesis repository after initialization to avoid circular dependency
        projectRepo.setHypothesisRepository(hybridHypothesisRepository)
        projectRepo
    }
    // TODO: Create HybridExperimentRepository
    // val hybridExperimentRepository by lazy {
    //     HybridExperimentRepository(database.experimentDao(), firebaseExperimentRepository, firebaseMappingRepository)
    // }
    
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
    
    // Legacy Local-only Repositories (for backward compatibility during migration)
    val projectRepository by lazy { ProjectRepository(database.projectDao()) }
    val hypothesisRepository by lazy { HypothesisRepository(database.hypothesisDao()) }
    val experimentRepository by lazy { ExperimentRepository(database.experimentDao()) }
    val logEntryRepository by lazy { LogEntryRepository(database.logEntryDao()) }
    val noteRepository by lazy { NoteRepository(database.noteDao()) }
    val reminderRepository by lazy { ReminderRepository(database.reminderSettingsDao()) }
} 