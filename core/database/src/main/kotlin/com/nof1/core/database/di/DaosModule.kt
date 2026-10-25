package com.nof1.core.database.di

import com.nof1.core.database.Nof1Database
import com.nof1.core.database.dao.CheckInDao
import com.nof1.core.database.dao.DeletionDao
import com.nof1.core.database.dao.ExperimentDao
import com.nof1.core.database.dao.ExportDao
import com.nof1.core.database.dao.HypothesisDao
import com.nof1.core.database.dao.NoteDao
import com.nof1.core.database.dao.ProjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object DaosModule {
    @Provides
    fun providesProjectDao(database: Nof1Database): ProjectDao = database.projectDao()

    @Provides
    fun providesHypothesisDao(database: Nof1Database): HypothesisDao = database.hypothesisDao()

    @Provides
    fun providesExperimentDao(database: Nof1Database): ExperimentDao = database.experimentDao()

    @Provides
    fun providesCheckInDao(database: Nof1Database): CheckInDao = database.checkInDao()

    @Provides
    fun providesNoteDao(database: Nof1Database): NoteDao = database.noteDao()

    @Provides
    fun providesExportDao(database: Nof1Database): ExportDao = database.exportDao()

    @Provides
    fun providesDeletionDao(database: Nof1Database): DeletionDao = database.deletionDao()
}
