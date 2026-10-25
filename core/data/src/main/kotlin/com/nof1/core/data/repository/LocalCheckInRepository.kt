package com.nof1.core.data.repository

import com.nof1.core.database.dao.CheckInDao
import com.nof1.core.database.model.answerEntities
import com.nof1.core.database.model.asEntity
import com.nof1.core.database.model.asExternalModel
import com.nof1.core.model.CheckIn
import com.nof1.core.model.CheckInId
import com.nof1.core.model.ExperimentId
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class LocalCheckInRepository @Inject constructor(
    private val checkInDao: CheckInDao,
    private val clock: Clock,
) : CheckInRepository {
    override fun observeForExperiment(experimentId: ExperimentId): Flow<List<CheckIn>> =
        checkInDao.observeForExperiment(experimentId.value)
            .map { checkIns -> checkIns.map { it.asExternalModel() } }

    override suspend fun save(checkIn: CheckIn) =
        checkInDao.save(checkIn.asEntity(), checkIn.answerEntities())

    override suspend fun delete(id: CheckInId) = checkInDao.delete(id.value, clock.millis())
}
