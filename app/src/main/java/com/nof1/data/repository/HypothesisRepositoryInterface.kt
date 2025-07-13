package com.nof1.data.repository

import com.nof1.data.model.Hypothesis

/**
 * Common interface for hypothesis repositories to support both local-only and hybrid implementations.
 * This enables ViewModels to work with either HypothesisRepository or HybridHypothesisRepository.
 */
interface HypothesisRepositoryInterface {
    suspend fun insertHypothesis(hypothesis: Hypothesis): Long
    suspend fun updateHypothesis(hypothesis: Hypothesis)
    suspend fun deleteHypothesis(hypothesis: Hypothesis)
    suspend fun archiveHypothesis(hypothesis: Hypothesis)
}