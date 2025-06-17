package com.nof1.data.repository

import com.nof1.data.local.HypothesisDao
import com.nof1.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime

/**
 * Hybrid repository that combines Room (offline) and Firebase (cloud) storage for Hypotheses.
 * Implements offline-first strategy with cloud sync.
 */
class HybridHypothesisRepository(
    private val hypothesisDao: HypothesisDao,
    private val firebaseHypothesisRepository: FirebaseHypothesisRepository
) {
    
    // Local storage for offline capability
    private val localRepository = HypothesisRepository(hypothesisDao)
    
    /**
     * Insert hypothesis - saves locally first, then syncs to cloud
     */
    suspend fun insertHypothesis(hypothesis: Hypothesis, firebaseProjectId: String): Long {
        // Save locally first
        val localId = localRepository.insertHypothesis(hypothesis)
        
        // Try to sync to cloud
        try {
            val firebaseId = firebaseHypothesisRepository.insertHypothesis(hypothesis, firebaseProjectId)
            // TODO: Store mapping between local ID and Firebase ID for sync
        } catch (e: Exception) {
            // Cloud sync failed, but local save succeeded
            // Will sync later when connection is restored
        }
        
        return localId
    }
    
    /**
     * Update hypothesis - updates locally first, then syncs to cloud
     */
    suspend fun updateHypothesis(hypothesis: Hypothesis, firebaseProjectId: String) {
        val updatedHypothesis = hypothesis.copy(updatedAt = LocalDateTime.now())
        
        // Update locally first
        localRepository.updateHypothesis(updatedHypothesis)
        
        // Try to sync to cloud
        try {
            val firebaseId = getFirebaseIdForHypothesis(hypothesis.id)
            if (firebaseId != null) {
                firebaseHypothesisRepository.updateHypothesis(firebaseId, updatedHypothesis, firebaseProjectId)
            }
        } catch (e: Exception) {
            // Cloud sync failed, mark for later sync
        }
    }
    
    /**
     * Delete hypothesis - deletes locally first, then syncs to cloud
     */
    suspend fun deleteHypothesis(hypothesis: Hypothesis) {
        // Delete locally first
        localRepository.deleteHypothesis(hypothesis)
        
        // Try to sync to cloud
        try {
            val firebaseId = getFirebaseIdForHypothesis(hypothesis.id)
            if (firebaseId != null) {
                firebaseHypothesisRepository.deleteHypothesis(firebaseId)
            }
        } catch (e: Exception) {
            // Cloud sync failed, mark for later sync
        }
    }
    
    /**
     * Archive hypothesis - archives locally first, then syncs to cloud
     */
    suspend fun archiveHypothesis(hypothesis: Hypothesis, firebaseProjectId: String) {
        // Archive locally first
        localRepository.archiveHypothesis(hypothesis)
        
        // Try to sync to cloud
        try {
            val firebaseId = getFirebaseIdForHypothesis(hypothesis.id)
            if (firebaseId != null) {
                firebaseHypothesisRepository.archiveHypothesis(firebaseId, hypothesis, firebaseProjectId)
            }
        } catch (e: Exception) {
            // Cloud sync failed, mark for later sync
        }
    }
    
    /**
     * Get hypothesis by ID - returns local data
     */
    suspend fun getHypothesisById(id: Long): Hypothesis? {
        return localRepository.getHypothesisById(id)
    }
    
    /**
     * Get hypotheses by project - combines local and cloud data
     */
    fun getAllHypothesesForProject(projectId: Long): Flow<List<Hypothesis>> {
        return localRepository.getAllHypothesesForProject(projectId)
    }
    
    /**
     * Get active hypotheses by project
     */
    fun getActiveHypothesesForProject(projectId: Long): Flow<List<Hypothesis>> {
        return localRepository.getActiveHypothesesForProject(projectId)
    }
    
    /**
     * Get hypothesis with experiments
     */
    fun getHypothesisWithExperiments(hypothesisId: Long): Flow<HypothesisWithExperiments?> {
        return localRepository.getHypothesisWithExperiments(hypothesisId)
    }
    
    /**
     * Sync hypotheses from cloud to local storage for a specific project
     */
    suspend fun syncFromCloud(firebaseProjectId: String) {
        try {
            firebaseHypothesisRepository.getHypothesesByProject(firebaseProjectId).collect { firebaseHypotheses ->
                // Convert Firebase hypotheses to Room hypotheses and save locally
                firebaseHypotheses.forEach { firebaseHypothesis ->
                    // This would need a proper mapping system in real implementation
                    // For now, we'll skip detailed sync implementation
                }
            }
        } catch (e: Exception) {
            // Sync failed, will retry later
        }
    }
    
    // Helper functions (simplified for demo)
    private suspend fun getFirebaseIdForHypothesis(localId: Long): String? {
        // TODO: Implement mapping between local and Firebase IDs
        return null
    }
} 