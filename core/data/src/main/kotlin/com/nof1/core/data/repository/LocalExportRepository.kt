package com.nof1.core.data.repository

import com.nof1.core.data.di.DefaultDispatcher
import com.nof1.core.database.dao.ExportDao
import com.nof1.core.database.model.asExternalModel
import com.nof1.core.export.CsvExport
import com.nof1.core.export.ExportData
import com.nof1.core.export.JsonExport
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal class LocalExportRepository @Inject constructor(
    private val exportDao: ExportDao,
    private val clock: Clock,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ExportRepository {
    override suspend fun exportJson(): String {
        val data = exportData()
        return withContext(defaultDispatcher) { JsonExport.encode(data, Instant.now(clock)) }
    }

    override suspend fun exportCheckInsCsv(): String {
        val data = exportData()
        return withContext(defaultDispatcher) { CsvExport.checkIns(data) }
    }

    private suspend fun exportData(): ExportData {
        val snapshot = exportDao.snapshot()
        return withContext(defaultDispatcher) {
            ExportData(
                projects = snapshot.projects.map { it.asExternalModel() },
                hypotheses = snapshot.hypotheses.map { it.asExternalModel() },
                experiments = snapshot.experiments.map { it.asExternalModel() },
                checkIns = snapshot.checkIns.map { it.asExternalModel() },
                notes = snapshot.notes.map { it.asExternalModel() },
            )
        }
    }
}
