package com.nof1.data.repository

import com.nof1.data.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/**
 * Temporary in-memory repository for projects to avoid Room database issues.
 */
class InMemoryProjectRepository {
    private val projects = MutableStateFlow<List<Project>>(emptyList())
    private var nextId = 1L

    suspend fun insertProject(project: Project): Long {
        val newProject = project.copy(id = nextId++)
        val currentProjects = projects.value.toMutableList()
        currentProjects.add(newProject)
        projects.value = currentProjects
        return newProject.id
    }

    suspend fun updateProject(project: Project) {
        val updatedProject = project.copy(updatedAt = LocalDateTime.now())
        val currentProjects = projects.value.toMutableList()
        val index = currentProjects.indexOfFirst { it.id == project.id }
        if (index != -1) {
            currentProjects[index] = updatedProject
            projects.value = currentProjects
        }
    }

    suspend fun deleteProject(project: Project) {
        val currentProjects = projects.value.toMutableList()
        currentProjects.removeAll { it.id == project.id }
        projects.value = currentProjects
    }

    suspend fun archiveProject(project: Project) {
        val archivedProject = project.copy(
            isArchived = true,
            updatedAt = LocalDateTime.now()
        )
        updateProject(archivedProject)
    }

    suspend fun getProjectById(id: Long): Project? {
        return projects.value.find { it.id == id }
    }

    fun getActiveProjects(): Flow<List<Project>> {
        return projects.map { list -> list.filter { !it.isArchived } }
    }

    fun getArchivedProjects(): Flow<List<Project>> {
        return projects.map { list -> list.filter { it.isArchived } }
    }

    fun getAllProjects(): Flow<List<Project>> {
        return projects
    }
} 