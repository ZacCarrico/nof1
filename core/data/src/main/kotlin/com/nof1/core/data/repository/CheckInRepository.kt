package com.nof1.core.data.repository

import com.nof1.core.model.CheckIn
import com.nof1.core.model.CheckInId
import com.nof1.core.model.ExperimentId
import kotlinx.coroutines.flow.Flow

interface CheckInRepository {
    /** The experiment's check-ins, by study day. */
    fun observeForExperiment(experimentId: ExperimentId): Flow<List<CheckIn>>

    /**
     * Adds the check-in, or replaces the stored one with the same ID, answers included. A study day
     * has at most one check-in: saving a different one for the same day fails.
     */
    suspend fun save(checkIn: CheckIn)

    suspend fun delete(id: CheckInId)
}
