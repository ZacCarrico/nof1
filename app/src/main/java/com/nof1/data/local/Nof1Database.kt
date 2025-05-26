package com.nof1.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nof1.data.model.Experiment
import com.nof1.data.model.Hypothesis
import com.nof1.data.model.LogEntry
import com.nof1.data.model.Project

/**
 * Main Room database for the application.
 */
@Database(
    entities = [
        Project::class,
        Hypothesis::class,
        Experiment::class,
        LogEntry::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class Nof1Database : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun hypothesisDao(): HypothesisDao
    abstract fun experimentDao(): ExperimentDao
    abstract fun logEntryDao(): LogEntryDao

    companion object {
        @Volatile
        private var INSTANCE: Nof1Database? = null

        fun getDatabase(context: Context): Nof1Database {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Nof1Database::class.java,
                    "nof1_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 