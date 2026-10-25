package com.nof1.core.data.repository

import com.nof1.core.database.dao.ExperimentDao
import com.nof1.core.database.model.asEntity
import com.nof1.core.database.model.asExternalModel
import com.nof1.core.database.model.questionEntities
import com.nof1.core.model.Experiment
import com.nof1.core.model.ExperimentId
import com.nof1.core.model.HypothesisId
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class LocalExperimentRepository @Inject constructor(
    private val experimentDao: ExperimentDao,
    private val clock: Clock,
) : ExperimentRepository {
    override fun observeForHypothesis(hypothesisId: HypothesisId): Flow<List<Experiment>> =
        experimentDao.observeForHypothesis(hypothesisId.value)
            .map { experiments -> experiments.map { it.asExternalModel() } }

    override fun observe(id: ExperimentId): Flow<Experiment?> =
        experimentDao.observe(id.value).map { it?.asExternalModel() }

    override fun observeStarted(): Flow<List<Experiment>> =
        experimentDao.observeStarted().map { experiments ->
            experiments.map { it.asExternalModel() }
        }

    override suspend fun save(experiment: Experiment) =
        experimentDao.save(experiment.asEntity(), experiment.questionEntities())

    override suspend fun delete(id: ExperimentId) = experimentDao.delete(id.value, clock.millis())
}
