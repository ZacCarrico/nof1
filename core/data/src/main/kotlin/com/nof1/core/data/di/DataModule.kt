package com.nof1.core.data.di

import com.nof1.core.data.repository.CheckInRepository
import com.nof1.core.data.repository.ExperimentRepository
import com.nof1.core.data.repository.ExportRepository
import com.nof1.core.data.repository.HypothesisRepository
import com.nof1.core.data.repository.LocalCheckInRepository
import com.nof1.core.data.repository.LocalExperimentRepository
import com.nof1.core.data.repository.LocalExportRepository
import com.nof1.core.data.repository.LocalHypothesisRepository
import com.nof1.core.data.repository.LocalNoteRepository
import com.nof1.core.data.repository.LocalProjectRepository
import com.nof1.core.data.repository.NoteRepository
import com.nof1.core.data.repository.ProjectRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindsProjectRepository(repository: LocalProjectRepository): ProjectRepository

    @Binds
    abstract fun bindsHypothesisRepository(
        repository: LocalHypothesisRepository,
    ): HypothesisRepository

    @Binds
    abstract fun bindsExperimentRepository(
        repository: LocalExperimentRepository,
    ): ExperimentRepository

    @Binds
    abstract fun bindsCheckInRepository(repository: LocalCheckInRepository): CheckInRepository

    @Binds
    abstract fun bindsNoteRepository(repository: LocalNoteRepository): NoteRepository

    @Binds
    abstract fun bindsExportRepository(repository: LocalExportRepository): ExportRepository
}
