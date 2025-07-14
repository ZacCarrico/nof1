package com.nof1.data.repository

import com.nof1.data.model.Hypothesis

/**
 * Interface for hypothesis repository operations.
 * Updated to work with Firebase-only implementation.
 */
interface HypothesisRepositoryInterface {
    /**
     * Insert a new hypothesis
     * @return Firebase document ID if successful, null otherwise
     */
    suspend fun insertHypothesis(hypothesis: Hypothesis): String?
    
    /**
     * Update an existing hypothesis
     * @return true if successful, false otherwise
     */
    suspend fun updateHypothesis(hypothesis: Hypothesis): Boolean
    
    /**
     * Delete a hypothesis
     * @return true if successful, false otherwise
     */
    suspend fun deleteHypothesis(hypothesis: Hypothesis): Boolean
    
    /**
     * Archive a hypothesis
     * @return true if successful, false otherwise
     */
    suspend fun archiveHypothesis(hypothesis: Hypothesis): Boolean
}