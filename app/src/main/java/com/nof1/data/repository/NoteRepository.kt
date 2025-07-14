package com.nof1.data.repository

import com.nof1.data.model.*
import com.nof1.utils.ImageUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * Firebase-only repository for accessing Note data.
 * This replaces the hybrid repository pattern.
 */
class NoteRepository : BaseFirebaseRepository() {
    
    private val notesCollection = firestore.collection("notes")
    
    /**
     * Insert a new note
     */
    suspend fun insertNote(note: Note): String? {
        val userId = requireUserId()
        val firebaseNote = note.copy(userId = userId)
        return addDocument(notesCollection, firebaseNote)
    }
    
    /**
     * Update an existing note
     */
    suspend fun updateNote(note: Note): Boolean {
        val userId = requireUserId()
        
        // Get the old note to check if we need to clean up old image
        val oldNote = getNoteById(note.id)
        val updatedNote = note.copy(userId = userId)
        
        val updateResult = updateDocument(notesCollection, note.id, updatedNote)
        
        // Clean up old image if it was replaced with a different one
        if (updateResult) {
            oldNote?.imagePath?.let { oldImagePath ->
                if (oldImagePath != note.imagePath) {
                    ImageUtils.deleteImage(oldImagePath)
                }
            }
        }
        
        return updateResult
    }
    
    /**
     * Delete a note
     */
    suspend fun deleteNote(note: Note): Boolean {
        val deleteResult = deleteDocument(notesCollection, note.id)
        
        // Clean up associated image
        if (deleteResult) {
            note.imagePath?.let { imagePath ->
                ImageUtils.deleteImage(imagePath)
            }
        }
        
        return deleteResult
    }
    
    /**
     * Get note by ID
     */
    suspend fun getNoteById(noteId: String): Note? {
        return getDocumentById<Note>(notesCollection, noteId)
    }
    
    /**
     * Get all notes for a hypothesis
     */
    fun getNotesForHypothesis(hypothesisId: String): Flow<List<Note>> {
        val userId = requireUserId()
        return getCollectionAsFlow<Note>(notesCollection) { collection ->
            collection
                .whereEqualTo("hypothesisId", hypothesisId)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    /**
     * Get all notes for a project
     */
    fun getNotesForProject(projectId: String): Flow<List<Note>> {
        val userId = requireUserId()
        return getCollectionAsFlow<Note>(notesCollection) { collection ->
            collection
                .whereEqualTo("projectId", projectId)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    /**
     * Get notes for hypothesis synchronously (for cleanup operations)
     */
    suspend fun getNotesForHypothesisSync(hypothesisId: String): List<Note> {
        val userId = requireUserId()
        return try {
            notesCollection
                .whereEqualTo("hypothesisId", hypothesisId)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Note::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Delete all notes for a hypothesis
     */
    suspend fun deleteAllNotesForHypothesis(hypothesisId: String): Boolean {
        return try {
            // Get all notes first to clean up their images
            val notes = getNotesForHypothesisSync(hypothesisId)
            
            // Delete images
            notes.forEach { note ->
                note.imagePath?.let { imagePath ->
                    ImageUtils.deleteImage(imagePath)
                }
            }
            
            // Delete notes from Firebase
            val userId = requireUserId()
            val querySnapshot = notesCollection
                .whereEqualTo("hypothesisId", hypothesisId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            // Delete each document
            querySnapshot.documents.forEach { document ->
                document.reference.delete().await()
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
} 