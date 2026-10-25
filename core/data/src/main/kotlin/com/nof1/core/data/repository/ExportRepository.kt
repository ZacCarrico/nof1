package com.nof1.core.data.repository

interface ExportRepository {
    /** Everything the user has entered, as the contents of a JSON export file. */
    suspend fun exportJson(): String

    /** Every check-in answer, as the contents of a CSV file. */
    suspend fun exportCheckInsCsv(): String
}
