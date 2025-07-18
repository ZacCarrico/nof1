package com.nof1.data.repository

import com.nof1.data.local.ProjectDao
import com.nof1.data.model.Project
import com.nof1.data.model.ProjectWithHypotheses
import com.nof1.data.model.ProjectWithHypothesesAndExperiments
import com.nof1.data.model.ReminderEntityType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

/**
 * Repository for accessing Project data.
 */
class ProjectRepository(
    private val projectDao: ProjectDao
) {
    suspend fun insertProject(project: Project): Long {
        return projectDao.insert(project)
    }

    suspend fun updateProject(project: Project) {
        val updatedProject = project.copy(updatedAt = LocalDateTime.now())
        projectDao.update(updatedProject)
    }

    suspend fun deleteProject(project: Project) {
        // Get all hypotheses for this project before deleting
        val projectWithHypotheses = projectDao.getProjectWithHypotheses(project.id).first()
        
        // Clean up reminder settings for the project
        cleanupReminderSettings(ReminderEntityType.PROJECT, project.id)
        
        // Clean up reminder settings and notes for all hypotheses
        projectWithHypotheses?.hypotheses?.forEach { hypothesis ->
            cleanupReminderSettings(ReminderEntityType.HYPOTHESIS, hypothesis.id)
            cleanupNotesForHypothesis(hypothesis.id)
        }
        
        // Delete the project (CASCADE will handle hypotheses, experiments, log entries)
        projectDao.delete(project)
    }

    suspend fun archiveProject(project: Project) {
        val archivedProject = project.copy(
            isArchived = true,
            updatedAt = LocalDateTime.now()
        )
        projectDao.update(archivedProject)
    }

    suspend fun getProjectById(id: Long): Project? {
        return projectDao.getProjectById(id)
    }

    fun getActiveProjects(): Flow<List<Project>> {
        return projectDao.getActiveProjects()
    }

    fun getArchivedProjects(): Flow<List<Project>> {
        return projectDao.getArchivedProjects()
    }

    fun getAllProjects(): Flow<List<Project>> {
        return projectDao.getAllProjects()
    }

    fun getProjectWithHypotheses(projectId: Long): Flow<ProjectWithHypotheses?> {
        return projectDao.getProjectWithHypotheses(projectId)
    }

    fun getProjectWithHypothesesAndExperiments(projectId: Long): Flow<ProjectWithHypothesesAndExperiments?> {
        return projectDao.getProjectWithHypothesesAndExperiments(projectId)
    }

    fun getActiveProjectsWithHypotheses(): Flow<List<ProjectWithHypotheses>> {
        return projectDao.getActiveProjectsWithHypotheses()
    }

    fun getAllProjectsWithHypotheses(): Flow<List<ProjectWithHypotheses>> {
        return projectDao.getAllProjectsWithHypotheses()
    }
    
    /**
     * Clean up reminder settings for an entity
     */
    private suspend fun cleanupReminderSettings(entityType: ReminderEntityType, entityId: Long) {
        // This method will be called by repositories that have access to ReminderRepository
        // For now, we'll add a comment indicating this needs to be handled by the calling code
        // TODO: Consider dependency injection or repository composition pattern
    }
    
    /**
     * Clean up notes and their associated image files for a hypothesis
     */
    private suspend fun cleanupNotesForHypothesis(hypothesisId: Long) {
        // This method will be called by repositories that have access to NoteRepository
        // For now, we'll add a comment indicating this needs to be handled by the calling code
        // TODO: Consider dependency injection or repository composition pattern
    }
} 