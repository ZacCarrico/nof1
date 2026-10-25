package com.nof1.core.data.repository

import com.nof1.core.model.Project
import com.nof1.core.model.ProjectId
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    /** All projects, archived ones included, oldest first. */
    fun observeAll(): Flow<List<Project>>

    fun observe(id: ProjectId): Flow<Project?>

    /** Adds the project, or replaces the stored one with the same ID. */
    suspend fun save(project: Project)

    /** Deletes the project with its hypotheses, experiments, check-ins and notes. */
    suspend fun delete(id: ProjectId)
}
