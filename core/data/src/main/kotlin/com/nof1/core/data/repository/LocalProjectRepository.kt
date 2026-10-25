package com.nof1.core.data.repository

import com.nof1.core.database.dao.ProjectDao
import com.nof1.core.database.model.asEntity
import com.nof1.core.database.model.asExternalModel
import com.nof1.core.model.Project
import com.nof1.core.model.ProjectId
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class LocalProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val clock: Clock,
) : ProjectRepository {
    override fun observeAll(): Flow<List<Project>> =
        projectDao.observeAll().map { projects -> projects.map { it.asExternalModel() } }

    override fun observe(id: ProjectId): Flow<Project?> =
        projectDao.observe(id.value).map { it?.asExternalModel() }

    override suspend fun save(project: Project) = projectDao.upsert(project.asEntity())

    override suspend fun delete(id: ProjectId) = projectDao.delete(id.value, clock.millis())
}
