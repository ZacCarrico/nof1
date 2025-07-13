package com.nof1.utils

import android.util.Log
import com.nof1.data.repository.FirebaseMappingRepository
import com.nof1.data.repository.FirebaseProjectRepository
import com.nof1.data.repository.ProjectRepository
import kotlinx.coroutines.flow.first

/**
 * Debug helper for diagnosing sync issues.
 */
class SyncDebugHelper(
    private val authManager: AuthManager,
    private val localRepository: ProjectRepository,
    private val firebaseRepository: FirebaseProjectRepository,
    private val mappingRepository: FirebaseMappingRepository
) {
    
    private val tag = "SyncDebugHelper"
    
    /**
     * Comprehensive debug information about sync state
     */
    suspend fun debugSyncState(): String {
        val debugInfo = StringBuilder()
        
        // Check authentication
        debugInfo.append("=== AUTHENTICATION ===\n")
        debugInfo.append("Is authenticated: ${authManager.isAuthenticated}\n")
        debugInfo.append("Current user ID: ${authManager.currentUserId}\n")
        debugInfo.append("Current user email: ${authManager.currentUser?.email}\n")
        
        // Check local data
        debugInfo.append("\n=== LOCAL DATA ===\n")
        try {
            val localProjects = localRepository.getAllProjects().first()
            debugInfo.append("Local projects count: ${localProjects.size}\n")
            localProjects.forEach { project ->
                debugInfo.append("- Local project: ${project.name} (ID: ${project.id})\n")
            }
        } catch (e: Exception) {
            debugInfo.append("Error reading local data: ${e.message}\n")
        }
        
        // Check Firebase data
        debugInfo.append("\n=== FIREBASE DATA ===\n")
        try {
            // First check if Firebase is initialized
            try {
                val firebaseApp = com.google.firebase.FirebaseApp.getInstance()
                debugInfo.append("Firebase app initialized: ${firebaseApp.name}\n")
                debugInfo.append("Firebase project ID: ${firebaseApp.options.projectId}\n")
                debugInfo.append("Firebase app ID: ${firebaseApp.options.applicationId}\n")
            } catch (e: Exception) {
                debugInfo.append("Firebase initialization issue: ${e.message}\n")
            }
            
            if (authManager.isAuthenticated) {
                debugInfo.append("Attempting to read Firebase projects...\n")
                val firebaseProjects = firebaseRepository.getAllProjects().first()
                debugInfo.append("Firebase projects count: ${firebaseProjects.size}\n")
                firebaseProjects.forEach { project ->
                    debugInfo.append("- Firebase project: ${project.name} (ID: ${project.id})\n")
                }
            } else {
                debugInfo.append("Cannot check Firebase data: Not authenticated\n")
            }
        } catch (e: Exception) {
            debugInfo.append("Error reading Firebase data: ${e.message}\n")
            debugInfo.append("Full exception: ${e.javaClass.simpleName}: ${e.localizedMessage}\n")
            if (e.cause != null) {
                debugInfo.append("Caused by: ${e.cause?.javaClass?.simpleName}: ${e.cause?.message}\n")
            }
        }
        
        // Check mappings
        debugInfo.append("\n=== ID MAPPINGS ===\n")
        try {
            val mappings = mappingRepository.getAllMappingsForEntityType(
                FirebaseMappingRepository.ENTITY_TYPE_PROJECT
            )
            debugInfo.append("Project mappings count: ${mappings.size}\n")
            mappings.forEach { mapping ->
                debugInfo.append("- Mapping: Local ID ${mapping.localId} -> Firebase ID ${mapping.firebaseId}\n")
            }
        } catch (e: Exception) {
            debugInfo.append("Error reading mappings: ${e.message}\n")
        }
        
        val result = debugInfo.toString()
        Log.d(tag, result)
        return result
    }
    
    /**
     * Test creating a project and syncing it
     */
    suspend fun testProjectSync(): String {
        val debugInfo = StringBuilder()
        
        debugInfo.append("=== TESTING PROJECT SYNC ===\n")
        
        if (!authManager.isAuthenticated) {
            debugInfo.append("ERROR: Not authenticated. Cannot test sync.\n")
            return debugInfo.toString()
        }
        
        try {
            // Create a test project locally
            val testProject = com.nof1.data.model.Project(
                name = "Test Sync Project",
                goal = "Test if sync is working"
            )
            
            val localId = localRepository.insertProject(testProject)
            debugInfo.append("Created local project with ID: $localId\n")
            
            // Try to sync to Firebase
            try {
                val firebaseId = firebaseRepository.insertProject(testProject)
                if (firebaseId != null) {
                    debugInfo.append("Created Firebase project with ID: $firebaseId\n")
                    
                    // Store mapping
                    mappingRepository.storeMapping(
                        FirebaseMappingRepository.ENTITY_TYPE_PROJECT,
                        localId,
                        firebaseId
                    )
                    debugInfo.append("Stored mapping between local and Firebase IDs\n")
                    
                    // Verify mapping works
                    val retrievedFirebaseId = mappingRepository.getFirebaseId(
                        FirebaseMappingRepository.ENTITY_TYPE_PROJECT,
                        localId
                    )
                    debugInfo.append("Retrieved Firebase ID from mapping: $retrievedFirebaseId\n")
                    
                    debugInfo.append("SUCCESS: Sync test completed successfully!\n")
                    
                    // Clean up test data
                    firebaseRepository.deleteProject(firebaseId)
                    mappingRepository.deleteMappingByLocalId(
                        FirebaseMappingRepository.ENTITY_TYPE_PROJECT,
                        localId
                    )
                    localRepository.deleteProject(testProject.copy(id = localId))
                    debugInfo.append("Cleaned up test data\n")
                    
                } else {
                    debugInfo.append("ERROR: Firebase project creation returned null - check Android logs for details\n")
                    debugInfo.append("This usually indicates:\n")
                    debugInfo.append("1. Network connectivity issues\n")
                    debugInfo.append("2. Authentication problems (check if user is still logged in)\n")
                    debugInfo.append("3. Firestore security rules blocking the write\n")
                    debugInfo.append("4. Firebase project configuration issues\n")
                    debugInfo.append("5. Data validation failures in Firestore\n")
                    debugInfo.append("Check Android logs with tag 'BaseFirebaseRepository' for specific error details\n")
                }
            } catch (e: Exception) {
                debugInfo.append("ERROR: Exception during Firebase project creation: ${e.message}\n")
                debugInfo.append("Full exception: ${e.javaClass.simpleName}: ${e.localizedMessage}\n")
                if (e.cause != null) {
                    debugInfo.append("Caused by: ${e.cause?.javaClass?.simpleName}: ${e.cause?.message}\n")
                }
            }
            
        } catch (e: Exception) {
            debugInfo.append("ERROR: Sync test failed: ${e.message}\n")
            Log.e(tag, "Sync test failed", e)
        }
        
        val result = debugInfo.toString()
        Log.d(tag, result)
        return result
    }
} 