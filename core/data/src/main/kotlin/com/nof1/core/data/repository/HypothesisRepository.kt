package com.nof1.core.data.repository

import com.nof1.core.model.Hypothesis
import com.nof1.core.model.HypothesisId
import com.nof1.core.model.ProjectId
import kotlinx.coroutines.flow.Flow

interface HypothesisRepository {
    /** The project's hypotheses, archived ones included, oldest first. */
    fun observeForProject(projectId: ProjectId): Flow<List<Hypothesis>>

    fun observe(id: HypothesisId): Flow<Hypothesis?>

    /** Adds the hypothesis, or replaces the stored one with the same ID. */
    suspend fun save(hypothesis: Hypothesis)

    /** Deletes the hypothesis with its experiments, check-ins and notes. */
    suspend fun delete(id: HypothesisId)
}
