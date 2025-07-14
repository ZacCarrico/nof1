package com.nof1.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firebase-compatible version of Note for Firestore storage.
 */
data class FirebaseNote(
    @DocumentId
    val id: String = "",
    
    val hypothesisId: String = "",
    val projectId: String = "", // For easier querying
    val content: String = "",
    val imagePath: String? = null, // Optional path to attached image file
    val userId: String = "",
    
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    // No-argument constructor required by Firestore
    constructor() : this("", "", "", "", null, "", null, null)
} 