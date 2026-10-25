package com.nof1.core.data.repository

import com.nof1.core.model.Experiment
import com.nof1.core.model.ExperimentId
import com.nof1.core.model.HypothesisId
import kotlinx.coroutines.flow.Flow

interface ExperimentRepository {
    /** The hypothesis's experiments, archived ones included, oldest first. */
    fun observeForHypothesis(hypothesisId: HypothesisId): Flow<List<Experiment>>

    fun observe(id: ExperimentId): Flow<Experiment?>

    /** Experiments that have started and aren't archived, including finished ones. */
    fun observeStarted(): Flow<List<Experiment>>

    /**
     * Adds the experiment, or replaces the stored one with the same ID. Fails if that would remove
     * a question that already has answers.
     */
    suspend fun save(experiment: Experiment)

    /** Deletes the experiment with its check-ins and notes. */
    suspend fun delete(id: ExperimentId)
}
