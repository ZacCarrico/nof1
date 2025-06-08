package com.nof1

import android.app.Application
import com.nof1.data.local.Nof1Database
import com.nof1.utils.NotificationHelper
import com.nof1.data.repository.ExperimentRepository
import com.nof1.data.repository.HypothesisRepository
import com.nof1.data.repository.LogEntryRepository
import com.nof1.data.repository.NoteRepository
import com.nof1.data.repository.ProjectRepository
import com.nof1.data.repository.ReminderRepository

/**
 * Application class for the Nof1 app.
 */
class Nof1Application : Application() {
    val database by lazy { Nof1Database.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
    
    // Repositories
    val projectRepository by lazy { ProjectRepository(database.projectDao()) }
    val hypothesisRepository by lazy { HypothesisRepository(database.hypothesisDao()) }
    val experimentRepository by lazy { ExperimentRepository(database.experimentDao()) }
    val logEntryRepository by lazy { LogEntryRepository(database.logEntryDao()) }
    val noteRepository by lazy { NoteRepository(database.noteDao()) }
    val reminderRepository by lazy { ReminderRepository(database.reminderSettingsDao()) }
} 