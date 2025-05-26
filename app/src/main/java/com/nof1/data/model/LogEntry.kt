package com.nof1.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Represents a log entry for an experiment.
 * Users create log entries in response to experiment questions.
 */
@Entity(
    tableName = "log_entries",
    foreignKeys = [
        ForeignKey(
            entity = Experiment::class,
            parentColumns = ["id"],
            childColumns = ["experimentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("experimentId")]
)
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val experimentId: Long,
    val response: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val isFromNotification: Boolean = false
) 