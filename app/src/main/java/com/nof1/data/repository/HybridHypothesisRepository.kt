package com.nof1.data.repository

import com.nof1.data.local.HypothesisDao
import com.nof1.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

/**
 * Hybrid repository that combines Room (offline) and Firebase (cloud) storage for Hypotheses.
 * Implements offline-first strategy with cloud sync.
 */
class HybridHypothesisRepository(
    private val hypothesisDao: HypothesisDao,
    private val firebaseHypothesisRepository: FirebaseHypothesisRepository,
    private val mappingRepository: FirebaseMappingRepository
) : HypothesisRepositoryInterface {
    
    // Local storage for offline capability
    private val localRepository = HypothesisRepository(hypothesisDao)
    
    /**
     * Insert hypothesis - saves locally first, then syncs to cloud
     */
    suspend fun insertHypothesis(hypothesis: Hypothesis, projectId: Long): Long {
        // Save locally first
        val localId = localRepository.insertHypothesis(hypothesis)
        
        // Try to sync to cloud
        try {
            val firebaseProjectId = mappingRepository.getFirebaseId(
                FirebaseMappingRepository.ENTITY_TYPE_PROJECT,
                projectId
            )
            
            if (firebaseProjectId != null) {
                val firebaseId = firebaseHypothesisRepository.insertHypothesis(hypothesis, firebaseProjectId)
                if (firebaseId != null) {
                    // Store mapping between local ID and Firebase ID
                    mappingRepository.storeMapping(
                        FirebaseMappingRepository.ENTITY_TYPE_HYPOTHESIS,
                        localId,
                        firebaseId
                    )
                }
            }
        } catch (e: Exception) {
            // Cloud sync failed, but local save succeeded
            android.util.Log.e("HybridHypothesisRepository", "Failed to sync hypothesis to cloud: ${e.message}")
        }
        
        return localId
    }
    
    /**
     * Insert hypothesis (backward compatibility method) - uses projectId from hypothesis.projectId
     * This method maintains compatibility with legacy HypothesisRepository interface
     */
    override suspend fun insertHypothesis(hypothesis: Hypothesis): Long {
        return insertHypothesis(hypothesis, hypothesis.projectId)
    }
    
    /**
     * Update hypothesis - updates locally first, then syncs to cloud
     */
    override suspend fun updateHypothesis(hypothesis: Hypothesis) {
        val updatedHypothesis = hypothesis.copy(updatedAt = LocalDateTime.now())
        
        // Update locally first
        localRepository.updateHypothesis(updatedHypothesis)
        
        // Try to sync to cloud
        try {
            val firebaseId = mappingRepository.getFirebaseId(
                FirebaseMappingRepository.ENTITY_TYPE_HYPOTHESIS,
                hypothesis.id
            )
            val firebaseProjectId = mappingRepository.getFirebaseId(
                FirebaseMappingRepository.ENTITY_TYPE_PROJECT,
                hypothesis.projectId
            )
            
            if (firebaseId != null && firebaseProjectId != null) {
                firebaseHypothesisRepository.updateHypothesis(firebaseId, updatedHypothesis, firebaseProjectId)
            }
        } catch (e: Exception) {
            android.util.Log.e("HybridHypothesisRepository", "Failed to sync hypothesis update to cloud: ${e.message}")
        }
    }
    
    /**
     * Delete hypothesis - deletes locally first, then syncs to cloud
     */
    override suspend fun deleteHypothesis(hypothesis: Hypothesis) {
        // Delete locally first
        localRepository.deleteHypothesis(hypothesis)
        
        // Try to sync to cloud
        try {
            val firebaseId = mappingRepository.getFirebaseId(
                FirebaseMappingRepository.ENTITY_TYPE_HYPOTHESIS,
                hypothesis.id
            )
            if (firebaseId != null) {
                firebaseHypothesisRepository.deleteHypothesis(firebaseId)
                mappingRepository.deleteMappingByLocalId(
                    FirebaseMappingRepository.ENTITY_TYPE_HYPOTHESIS,
                    hypothesis.id
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("HybridHypothesisRepository", "Failed to sync hypothesis deletion to cloud: ${e.message}")
        }
    }
    
    /**
     * Archive hypothesis - archives locally first, then syncs to cloud
     */
    override suspend fun archiveHypothesis(hypothesis: Hypothesis) {
        // Archive locally first
        localRepository.archiveHypothesis(hypothesis)
        
        // Try to sync to cloud
        try {
            val firebaseId = mappingRepository.getFirebaseId(
                FirebaseMappingRepository.ENTITY_TYPE_HYPOTHESIS,
                hypothesis.id
            )
            val firebaseProjectId = mappingRepository.getFirebaseId(
                FirebaseMappingRepository.ENTITY_TYPE_PROJECT,
                hypothesis.projectId
            )
            
            if (firebaseId != null && firebaseProjectId != null) {
                firebaseHypothesisRepository.archiveHypothesis(firebaseId, hypothesis, firebaseProjectId)
            }
        } catch (e: Exception) {
            android.util.Log.e("HybridHypothesisRepository", "Failed to sync hypothesis archiving to cloud: ${e.message}")
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
     * Get active hypotheses by project - combines local and cloud data
     */
    fun getActiveHypothesesForProject(projectId: Long): Flow<List<Hypothesis>> {
        android.util.Log.d("HybridHypothesisRepository", "getActiveHypothesesForProject($projectId) called")
        return combine(
            localRepository.getActiveHypothesesForProject(projectId),
            getCloudHypothesesForProject(projectId)
        ) { localHypotheses, cloudHypotheses ->
            android.util.Log.d("HybridHypothesisRepository", "Combining hypotheses: local=${localHypotheses.size}, cloud=${cloudHypotheses.size}")
            // Merge and deduplicate hypotheses
            val merged = mergeHypotheses(localHypotheses, cloudHypotheses).filter { !it.isArchived }
            android.util.Log.d("HybridHypothesisRepository", "Active hypotheses result: ${merged.size}")
            merged
        }
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
    suspend fun syncFromCloud(firebaseProjectId: String, localProjectId: Long) {
        try {
            android.util.Log.d("HybridHypothesisRepository", "Starting hypothesis sync for project: $firebaseProjectId")
            // Use first() to get one-time snapshot instead of continuous listening
            val firebaseHypotheses = firebaseHypothesisRepository.getHypothesesByProject(firebaseProjectId).first()
            android.util.Log.d("HybridHypothesisRepository", "Syncing ${firebaseHypotheses.size} hypotheses from cloud")
            
            // Convert Firebase hypotheses to Room hypotheses and save locally
            firebaseHypotheses.forEach { firebaseHypothesis ->
                val localHypothesis = firebaseHypothesis.toHypothesis(localProjectId)
                android.util.Log.d("HybridHypothesisRepository", "Processing Firebase hypothesis: ${firebaseHypothesis.name} (${firebaseHypothesis.id})")
                
                // Check if hypothesis already exists locally
                val existingHypothesis = getHypothesisByFirebaseId(firebaseHypothesis.id)
                if (existingHypothesis == null) {
                    android.util.Log.d("HybridHypothesisRepository", "Inserting new hypothesis: ${localHypothesis.name}")
                    val localId = localRepository.insertHypothesis(localHypothesis)
                    // Store mapping
                    mappingRepository.storeMapping(
                        FirebaseMappingRepository.ENTITY_TYPE_HYPOTHESIS,
                        localId,
                        firebaseHypothesis.id
                    )
                } else {
                    android.util.Log.d("HybridHypothesisRepository", "Hypothesis already exists locally: ${existingHypothesis.name}")
                    // Update existing hypothesis if cloud version is newer
                    if (isCloudVersionNewer(firebaseHypothesis, existingHypothesis)) {
                        android.util.Log.d("HybridHypothesisRepository", "Updating existing hypothesis from cloud: ${localHypothesis.name}")
                        localRepository.updateHypothesis(localHypothesis.copy(id = existingHypothesis.id))
                    }
                }
            }
            android.util.Log.d("HybridHypothesisRepository", "Hypothesis sync completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("HybridHypothesisRepository", "Failed to sync hypotheses from cloud: ${e.message}", e)
        }
    }
    
    // Helper functions
    
    private fun getCloudHypothesesForProject(projectId: Long): Flow<List<Hypothesis>> = flow {
        try {
            // Get Firebase project ID from local project ID
            val firebaseProjectId = mappingRepository.getFirebaseId(
                FirebaseMappingRepository.ENTITY_TYPE_PROJECT,
                projectId
            )
            
            if (firebaseProjectId != null) {
                android.util.Log.d("HybridHypothesisRepository", "Getting cloud hypotheses for project: $firebaseProjectId")
                firebaseHypothesisRepository.getActiveHypothesesByProject(firebaseProjectId).collect { firebaseHypotheses ->
                    android.util.Log.d("HybridHypothesisRepository", "Firebase returned ${firebaseHypotheses.size} hypotheses")
                    val localHypotheses = firebaseHypotheses.map { it.toHypothesis(projectId) }
                    emit(localHypotheses)
                }
            } else {
                android.util.Log.d("HybridHypothesisRepository", "No Firebase project ID found for local project $projectId")
                emit(emptyList<Hypothesis>())
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            android.util.Log.d("HybridHypothesisRepository", "Cloud hypotheses collection cancelled")
            // Don't log as error - this is expected when scope is cancelled
            throw e
        } catch (e: Exception) {
            android.util.Log.e("HybridHypothesisRepository", "Error getting cloud hypotheses: ${e.message}", e)
            emit(emptyList<Hypothesis>())
        }
    }
    
    private fun mergeHypotheses(localHypotheses: List<Hypothesis>, cloudHypotheses: List<Hypothesis>): List<Hypothesis> {
        // Simple merge strategy - prefer local data, add cloud-only items
        val mergedMap = mutableMapOf<String, Hypothesis>()
        
        // Add local hypotheses first
        localHypotheses.forEach { hypothesis ->
            mergedMap[hypothesis.name] = hypothesis // Use name as key for simplicity
        }
        
        // Add cloud hypotheses that don't exist locally
        cloudHypotheses.forEach { cloudHypothesis ->
            if (!mergedMap.containsKey(cloudHypothesis.name)) {
                mergedMap[cloudHypothesis.name] = cloudHypothesis
            }
        }
        
        return mergedMap.values.sortedByDescending { it.createdAt }
    }
    
    private suspend fun getHypothesisByFirebaseId(firebaseId: String): Hypothesis? {
        val localId = mappingRepository.getLocalId(
            FirebaseMappingRepository.ENTITY_TYPE_HYPOTHESIS,
            firebaseId
        )
        return localId?.let { localRepository.getHypothesisById(it) }
    }
    
    private fun isCloudVersionNewer(firebaseHypothesis: FirebaseHypothesis, localHypothesis: Hypothesis): Boolean {
        return firebaseHypothesis.updatedAt?.toDate()?.let { firebaseDate ->
            val firebaseDateTime = java.time.LocalDateTime.ofInstant(
                firebaseDate.toInstant(), 
                java.time.ZoneId.systemDefault()
            )
            firebaseDateTime.isAfter(localHypothesis.updatedAt)
        } ?: false
    }
}