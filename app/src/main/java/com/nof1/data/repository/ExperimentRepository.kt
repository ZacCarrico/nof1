package com.nof1.data.repository

import com.nof1.data.local.ExperimentDao
import com.nof1.data.model.Experiment
import com.nof1.data.model.ExperimentWithLogs
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Repository for accessing Experiment data.
 */
class ExperimentRepository(
    private val experimentDao: ExperimentDao
) {
    suspend fun insertExperiment(experiment: Experiment): Long {
        return experimentDao.insert(experiment)
    }

    suspend fun updateExperiment(experiment: Experiment) {
        val updatedExperiment = experiment.copy(updatedAt = LocalDateTime.now())
        experimentDao.update(updatedExperiment)
    }

    suspend fun deleteExperiment(experiment: Experiment) {
        experimentDao.delete(experiment)
    }

    suspend fun archiveExperiment(experiment: Experiment) {
        val archivedExperiment = experiment.copy(
            isArchived = true,
            updatedAt = LocalDateTime.now()
        )
        experimentDao.update(archivedExperiment)
    }

    suspend fun updateLastLoggedAt(experimentId: Long) {
        experimentDao.updateLastLoggedAt(experimentId, LocalDateTime.now())
    }

    suspend fun getExperimentById(id: Long): Experiment? {
        return experimentDao.getExperimentById(id)
    }

    fun getActiveExperimentsForHypothesis(hypothesisId: Long): Flow<List<Experiment>> {
        return experimentDao.getActiveExperimentsForHypothesis(hypothesisId)
    }

    fun getAllExperimentsForHypothesis(hypothesisId: Long): Flow<List<Experiment>> {
        return experimentDao.getAllExperimentsForHypothesis(hypothesisId)
    }

    fun getExperimentWithLogs(experimentId: Long): Flow<ExperimentWithLogs?> {
        return experimentDao.getExperimentWithLogs(experimentId)
    }

    fun getActiveExperimentsWithLogsForHypothesis(hypothesisId: Long): Flow<List<ExperimentWithLogs>> {
        return experimentDao.getActiveExperimentsWithLogsForHypothesis(hypothesisId)
    }

    fun getExperimentsWithNotificationsEnabled(): Flow<List<Experiment>> {
        return experimentDao.getExperimentsWithNotificationsEnabled()
    }
} 