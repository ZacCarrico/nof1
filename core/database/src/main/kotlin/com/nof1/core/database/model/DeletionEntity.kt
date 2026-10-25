package com.nof1.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity

/**
 * A record that the user deleted something, so a future sync can delete it on other devices too.
 * It holds only the type and ID, never the deleted content. Records deleted along with their
 * parent, such as a project's hypotheses, aren't listed separately.
 */
@Entity(tableName = "deletions", primaryKeys = ["entity_type", "entity_id"])
data class DeletionEntity(
    /** One of the type constants below. */
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long,
) {
    companion object {
        const val PROJECT = "project"
        const val HYPOTHESIS = "hypothesis"
        const val EXPERIMENT = "experiment"
        const val CHECK_IN = "check_in"
        const val NOTE = "note"
    }
}
