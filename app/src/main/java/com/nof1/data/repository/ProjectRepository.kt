package com.nof1.data.repository

import com.nof1.data.local.ProjectDao
import com.nof1.data.model.Project
import com.nof1.data.model.ProjectWithHypotheses
import com.nof1.data.model.ProjectWithHypothesesAndExperiments
import kotlinx.coroutines.flow.Flow
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
} 