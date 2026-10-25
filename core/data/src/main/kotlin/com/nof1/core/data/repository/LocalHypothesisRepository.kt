package com.nof1.core.data.repository

import com.nof1.core.database.dao.HypothesisDao
import com.nof1.core.database.model.asEntity
import com.nof1.core.database.model.asExternalModel
import com.nof1.core.model.Hypothesis
import com.nof1.core.model.HypothesisId
import com.nof1.core.model.ProjectId
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class LocalHypothesisRepository @Inject constructor(
    private val hypothesisDao: HypothesisDao,
    private val clock: Clock,
) : HypothesisRepository {
    override fun observeForProject(projectId: ProjectId): Flow<List<Hypothesis>> =
        hypothesisDao.observeForProject(projectId.value)
            .map { hypotheses -> hypotheses.map { it.asExternalModel() } }

    override fun observe(id: HypothesisId): Flow<Hypothesis?> =
        hypothesisDao.observe(id.value).map { it?.asExternalModel() }

    override suspend fun save(hypothesis: Hypothesis) = hypothesisDao.upsert(hypothesis.asEntity())

    override suspend fun delete(id: HypothesisId) = hypothesisDao.delete(id.value, clock.millis())
}
