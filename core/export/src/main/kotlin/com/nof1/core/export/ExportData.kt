package com.nof1.core.export

import com.nof1.core.model.CheckIn
import com.nof1.core.model.Experiment
import com.nof1.core.model.Hypothesis
import com.nof1.core.model.Note
import com.nof1.core.model.Project

/** Everything the user has entered, to be written out by [JsonExport] or [CsvExport]. */
data class ExportData(
    val projects: List<Project>,
    val hypotheses: List<Hypothesis>,
    val experiments: List<Experiment>,
    val checkIns: List<CheckIn>,
    val notes: List<Note>,
)
