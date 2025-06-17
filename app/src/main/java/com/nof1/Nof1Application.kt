package com.nof1

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
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
    
    // Hybrid Repositories (combining local + cloud)
    val hybridProjectRepository by lazy { 
        HybridProjectRepository(database.projectDao(), firebaseProjectRepository) 
    }
    val hybridHypothesisRepository by lazy {
        HybridHypothesisRepository(database.hypothesisDao(), firebaseHypothesisRepository)
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize WorkManager
        val configuration = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
        WorkManager.initialize(this, configuration)
        
        NotificationHelper.createNotificationChannel(this)
    }
    
    // Legacy Local-only Repositories (for backward compatibility during migration)
    val projectRepository by lazy { ProjectRepository(database.projectDao()) }
    val hypothesisRepository by lazy { HypothesisRepository(database.hypothesisDao()) }
    val experimentRepository by lazy { ExperimentRepository(database.experimentDao()) }
    val logEntryRepository by lazy { LogEntryRepository(database.logEntryDao()) }
    val noteRepository by lazy { NoteRepository(database.noteDao()) }
    val reminderRepository by lazy { ReminderRepository(database.reminderSettingsDao()) }
} 