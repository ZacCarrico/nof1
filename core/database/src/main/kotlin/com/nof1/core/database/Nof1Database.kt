package com.nof1.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.nof1.core.database.dao.CheckInDao
import com.nof1.core.database.dao.DeletionDao
import com.nof1.core.database.dao.ExperimentDao
import com.nof1.core.database.dao.ExportDao
import com.nof1.core.database.dao.HypothesisDao
import com.nof1.core.database.dao.NoteDao
import com.nof1.core.database.dao.ProjectDao
import com.nof1.core.database.model.AnswerEntity
import com.nof1.core.database.model.CheckInEntity
import com.nof1.core.database.model.DeletionEntity
import com.nof1.core.database.model.ExperimentEntity
import com.nof1.core.database.model.HypothesisEntity
import com.nof1.core.database.model.NoteEntity
import com.nof1.core.database.model.ProjectEntity
import com.nof1.core.database.model.QuestionEntity

/**
 * The app's database: the single source of truth for everything the user enters.
 *
 * Storage conventions: IDs are UUID strings; timestamps are milliseconds since the epoch (UTC);
 * dates and times of day are ISO-8601 text ("2026-10-25", "21:00"); enums are stored by name.
 * Deleted rows are removed, not flagged, and [DeletionEntity] records what was deleted.
 *
 * Every schema version is exported to `core/database/schemas`. Changing an entity means bumping
 * [version][Database.version] and adding a migration with a test.
 */
@Database(
    entities = [
        ProjectEntity::class,
        HypothesisEntity::class,
        ExperimentEntity::class,
        QuestionEntity::class,
        CheckInEntity::class,
        AnswerEntity::class,
        NoteEntity::class,
        DeletionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class Nof1Database : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    abstract fun hypothesisDao(): HypothesisDao

    abstract fun experimentDao(): ExperimentDao

    abstract fun checkInDao(): CheckInDao

    abstract fun noteDao(): NoteDao

    abstract fun exportDao(): ExportDao

    abstract fun deletionDao(): DeletionDao

    companion object {
        const val FILE_NAME = "nof1.db"
    }
}
