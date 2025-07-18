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
            android.util.Log.d("HybridHypothesisRepository", "Combining hypotheses for project $projectId: local=${localHypotheses.size}, cloud=${cloudHypotheses.size}")
            if (localHypotheses.isNotEmpty()) {
                android.util.Log.d("HybridHypothesisRepository", "Local hypotheses names: ${localHypotheses.map { it.name }}")
            }
            if (cloudHypotheses.isNotEmpty()) {
                android.util.Log.d("HybridHypothesisRepository", "Cloud hypotheses names: ${cloudHypotheses.map { it.name }}")
            }
            
            // Merge and deduplicate hypotheses
            val merged = mergeHypotheses(localHypotheses, cloudHypotheses).filter { !it.isArchived }
            android.util.Log.d("HybridHypothesisRepository", "After merging and filtering archived: ${merged.size} active hypotheses")
            if (merged.isNotEmpty()) {
                android.util.Log.d("HybridHypothesisRepository", "Final active hypotheses names: ${merged.map { it.name }}")
            }
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
            android.util.Log.d("HybridHypothesisRepository", "getCloudHypothesesForProject called with local projectId: $projectId")
            
            // Get Firebase project ID from local project ID
            val firebaseProjectId = mappingRepository.getFirebaseId(
                FirebaseMappingRepository.ENTITY_TYPE_PROJECT,
                projectId
            )
            
            android.util.Log.d("HybridHypothesisRepository", "Local project ID $projectId maps to Firebase project ID: $firebaseProjectId")
            
            if (firebaseProjectId != null) {
                android.util.Log.d("HybridHypothesisRepository", "Getting cloud hypotheses for Firebase project: $firebaseProjectId")
                firebaseHypothesisRepository.getActiveHypothesesByProject(firebaseProjectId).collect { firebaseHypotheses ->
                    android.util.Log.d("HybridHypothesisRepository", "Firebase returned ${firebaseHypotheses.size} hypotheses for project $firebaseProjectId")
                    if (firebaseHypotheses.isNotEmpty()) {
                        android.util.Log.d("HybridHypothesisRepository", "Firebase hypotheses names: ${firebaseHypotheses.map { it.name }}")
                    }
                    val localHypotheses = firebaseHypotheses.map { it.toHypothesis(projectId) }
                    android.util.Log.d("HybridHypothesisRepository", "Converted to ${localHypotheses.size} local hypotheses, emitting...")
                    emit(localHypotheses)
                }
            } else {
                android.util.Log.w("HybridHypothesisRepository", "No Firebase project ID mapping found for local project $projectId - emitting empty list")
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
        android.util.Log.d("HybridHypothesisRepository", "mergeHypotheses called: local=${localHypotheses.size}, cloud=${cloudHypotheses.size}")
        
        // Simple merge strategy - prefer local data, add cloud-only items
        val mergedMap = mutableMapOf<String, Hypothesis>()
        
        // Add local hypotheses first
        localHypotheses.forEach { hypothesis ->
            android.util.Log.d("HybridHypothesisRepository", "Adding local hypothesis to merge: ${hypothesis.name}")
            mergedMap[hypothesis.name] = hypothesis // Use name as key for simplicity
        }
        
        // Add cloud hypotheses that don't exist locally
        cloudHypotheses.forEach { cloudHypothesis ->
            if (!mergedMap.containsKey(cloudHypothesis.name)) {
                android.util.Log.d("HybridHypothesisRepository", "Adding cloud-only hypothesis to merge: ${cloudHypothesis.name}")
                mergedMap[cloudHypothesis.name] = cloudHypothesis
            } else {
                android.util.Log.d("HybridHypothesisRepository", "Skipping cloud hypothesis (already exists locally): ${cloudHypothesis.name}")
            }
        }
        
        val result = mergedMap.values.sortedByDescending { it.createdAt }
        android.util.Log.d("HybridHypothesisRepository", "mergeHypotheses result: ${result.size} hypotheses")
        return result
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