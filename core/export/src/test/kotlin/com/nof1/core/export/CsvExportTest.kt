package com.nof1.core.export

import com.nof1.core.model.Answer
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsvExportTest {
    @Test
    fun startsWithTheHeader() {
        val lines = CsvExport.checkIns(exportData(checkIns = emptyList())).lines()

        assertEquals(
            "experiment_id,experiment_title,study_day,period,pair,condition,condition_name," +
                "settling_in,followed_plan,question_id,question,answer_type,value,unit," +
                "recorded_at,time_zone",
            lines.first(),
        )
    }

    @Test
    fun hasOneRowPerAnswerInQuestionOrder() {
        val rows = parse(CsvExport.checkIns(exportData(checkIns = listOf(checkIn(2))))).drop(1)

        assertEquals(
            listOf(
                listOf("q-sleep", "How rested do you feel?", "rating", "6", ""),
                listOf("q-caffeine", "How much caffeine did you have?", "number", "95", "mg"),
                listOf("q-nap", "Did you nap?", "yes_no", "false", ""),
                listOf("q-remarks", "Anything unusual?", "free_text", "Late dinner", ""),
            ),
            rows.map { it.subList(9, 14) },
        )
        for (row in rows) {
            assertEquals(CsvExport.COLUMNS.size, row.size)
            assertEquals(listOf("2026-03-03T02:05:00Z", "America/New_York"), row.subList(14, 16))
        }
    }

    @Test
    fun describesEachDaysPeriod() {
        val days = listOf(2, 3, 5, 8, 13)
        val data =
            exportData(checkIns = days.map { checkIn(it, mapOf(SLEEP.id to Answer.Rating(5))) })

        val rows = parse(CsvExport.checkIns(data)).drop(1)

        // study_day, period, pair, condition, condition_name, settling_in
        assertEquals(
            listOf(
                listOf("2026-03-02", "1", "1", "baseline", "Usual evenings", "true"),
                listOf("2026-03-03", "1", "1", "baseline", "Usual evenings", "false"),
                listOf("2026-03-05", "2", "1", "intervention", "No screens", "true"),
                listOf("2026-03-08", "3", "2", "intervention", "No screens", "true"),
                listOf("2026-03-13", "4", "2", "baseline", "Usual evenings", "false"),
            ),
            rows.map { it.subList(2, 8) },
        )
    }

    @Test
    fun daysOutsideTheStudyHaveNoPeriod() {
        val stopped = experiment().copy(stoppedOn = LocalDate.of(2026, 3, 4))
        val data = exportData(
            experiments = listOf(stopped),
            checkIns = listOf(checkIn(5, mapOf(SLEEP.id to Answer.Rating(5)))),
        )

        val row = parse(CsvExport.checkIns(data))[1]

        assertEquals(listOf("2026-03-05", "", "", "", "", ""), row.subList(2, 8))
    }

    @Test
    fun checkInWithoutAnswersKeepsItsAdherenceAnswer() {
        val data =
            exportData(checkIns = listOf(checkIn(2, answers = emptyMap(), followedPlan = false)))

        val rows = parse(CsvExport.checkIns(data)).drop(1)

        assertEquals(1, rows.size)
        assertEquals("false", rows[0][8])
        assertEquals(List(5) { "" }, rows[0].subList(9, 14))
    }

    @Test
    fun missingAdherenceAnswerIsEmpty() {
        val data = exportData(checkIns = listOf(checkIn(2, followedPlan = null)))

        assertEquals("", parse(CsvExport.checkIns(data))[1][8])
    }

    @Test
    fun quotesFieldsWithCommasQuotesAndLineBreaks() {
        val text = "Coffee, \"a big one\"\nthen bed"
        val data =
            exportData(checkIns = listOf(checkIn(2, mapOf(REMARKS.id to Answer.FreeText(text)))))

        val csv = CsvExport.checkIns(data)

        assertTrue("\"Coffee, \"\"a big one\"\"\nthen bed\"" in csv, csv)
        assertEquals(text, parse(csv)[1][12])
    }

    @Test
    fun quotesOnlyTheFieldsThatNeedIt() {
        assertEquals("plain text", Csv.escape("plain text"))
        assertEquals("\"a, b\"", Csv.escape("a, b"))
        assertEquals("\"say \"\"hi\"\"\"", Csv.escape("say \"hi\""))
        assertEquals("\"line\nbreak\"", Csv.escape("line\nbreak"))
        assertEquals("\"carriage\rreturn\"", Csv.escape("carriage\rreturn"))
    }

    @Test
    fun userTextThatLooksLikeAFormulaIsMadePlainText() {
        val data = exportData(
            experiments = listOf(experiment(title = "=HYPERLINK(\"https://example.com\")")),
            checkIns = listOf(checkIn(2, mapOf(REMARKS.id to Answer.FreeText("+1 hour of sleep")))),
        )

        val row = parse(CsvExport.checkIns(data))[1]

        assertEquals("'=HYPERLINK(\"https://example.com\")", row[1])
        assertEquals("'+1 hour of sleep", row[12])
    }

    @Test
    fun guardsEveryFormulaStart() {
        for (start in listOf("=", "+", "-", "@", "\t", "\r", "\n")) {
            assertEquals("'${start}x", Csv.userText("${start}x"))
        }
        assertEquals("1+1", Csv.userText("1+1"))
        assertEquals("", Csv.userText(""))
    }

    @Test
    fun negativeNumbersAreNotTreatedAsFormulas() {
        val data =
            exportData(
                checkIns = listOf(checkIn(2, mapOf(CAFFEINE.id to Answer.NumberWithUnit(-3.25)))),
            )

        assertEquals("-3.25", parse(CsvExport.checkIns(data))[1][12])
    }

    @Test
    fun formatsNumbersWithoutExponentsOrTrailingZeros() {
        assertEquals("7.5", Csv.number(7.5))
        assertEquals("8", Csv.number(8.0))
        assertEquals("0", Csv.number(0.0))
        assertEquals("0", Csv.number(-0.0))
        assertEquals("0.0001", Csv.number(0.0001))
        assertEquals("12000000", Csv.number(1.2e7))
        assertEquals("-3.25", Csv.number(-3.25))
    }

    @Test
    fun linesEndWithCrLf() {
        val csv = CsvExport.checkIns(exportData(checkIns = listOf(checkIn(2))))

        assertTrue(csv.endsWith("\r\n"))
        assertEquals(5, csv.split("\r\n").size - 1)
        assertTrue(csv.replace("\r\n", "").none { it == '\r' || it == '\n' })
    }

    @Test
    fun skipsCheckInsOfUnknownExperiments() {
        val data = exportData(experiments = emptyList(), checkIns = listOf(checkIn(2)))

        assertEquals(1, parse(CsvExport.checkIns(data)).size)
    }

    /** A minimal RFC 4180 reader, to check what a spreadsheet would see. */
    private fun parse(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < csv.length) {
            val c = csv[i]
            when {
                quoted && c == '"' && csv.getOrNull(i + 1) == '"' -> {
                    field.append('"')
                    i++
                }

                quoted && c == '"' -> quoted = false

                quoted -> field.append(c)

                c == '"' -> quoted = true

                c == ',' -> {
                    row.add(field.toString())
                    field.clear()
                }

                c == '\r' && csv.getOrNull(i + 1) == '\n' -> {
                    row.add(field.toString())
                    field.clear()
                    rows.add(row)
                    row = mutableListOf()
                    i++
                }

                else -> field.append(c)
            }
            i++
        }
        return rows
    }
}
