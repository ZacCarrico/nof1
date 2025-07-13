package com.nof1.data.repository

import com.nof1.data.local.HypothesisDao
import com.nof1.data.model.Hypothesis
import com.nof1.data.model.HypothesisWithExperiments
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Repository for accessing Hypothesis data.
 */
class HypothesisRepository(
    private val hypothesisDao: HypothesisDao
) : HypothesisRepositoryInterface {
    override suspend fun insertHypothesis(hypothesis: Hypothesis): Long {
        return hypothesisDao.insert(hypothesis)
    }

    override suspend fun updateHypothesis(hypothesis: Hypothesis) {
        val updatedHypothesis = hypothesis.copy(updatedAt = LocalDateTime.now())
        hypothesisDao.update(updatedHypothesis)
    }

    override suspend fun deleteHypothesis(hypothesis: Hypothesis) {
        hypothesisDao.delete(hypothesis)
    }

    override suspend fun archiveHypothesis(hypothesis: Hypothesis) {
        val archivedHypothesis = hypothesis.copy(
            isArchived = true,
            updatedAt = LocalDateTime.now()
        )
        hypothesisDao.update(archivedHypothesis)
    }

    suspend fun getHypothesisById(id: Long): Hypothesis? {
        return hypothesisDao.getHypothesisById(id)
    }

    fun getActiveHypothesesForProject(projectId: Long): Flow<List<Hypothesis>> {
        return hypothesisDao.getActiveHypothesesForProject(projectId)
    }

    fun getAllHypothesesForProject(projectId: Long): Flow<List<Hypothesis>> {
        return hypothesisDao.getAllHypothesesForProject(projectId)
    }

    fun getHypothesisWithExperiments(hypothesisId: Long): Flow<HypothesisWithExperiments?> {
        return hypothesisDao.getHypothesisWithExperiments(hypothesisId)
    }

    fun getActiveHypothesesWithExperimentsForProject(projectId: Long): Flow<List<HypothesisWithExperiments>> {
        return hypothesisDao.getActiveHypothesesWithExperimentsForProject(projectId)
    }

    fun getAllHypothesesWithExperimentsForProject(projectId: Long): Flow<List<HypothesisWithExperiments>> {
        return hypothesisDao.getAllHypothesesWithExperimentsForProject(projectId)
    }
} 