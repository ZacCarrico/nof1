package com.nof1.core.export

import com.nof1.core.model.Answer
import com.nof1.core.model.QuestionId
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class JsonExportTest {
    private val exportedAt = Instant.parse("2026-10-25T02:30:00Z")

    @Test
    fun matchesTheVersion1Format() {
        val data = exportData(
            checkIns = listOf(checkIn(2), checkIn(6, answers = emptyMap(), followedPlan = null)),
        )

        assertEquals(resource("export-v1.json").trimEnd('\n'), JsonExport.encode(data, exportedAt))
    }

    @Test
    fun answersAreListedInQuestionOrder() {
        val json = parse(JsonExport.encode(exportData(), exportedAt))

        val answers = json.array("checkIns")[0].jsonObject.array("answers")

        assertEquals(
            listOf("q-sleep", "q-caffeine", "q-nap", "q-remarks"),
            answers.map { it.jsonObject.string("questionId") },
        )
    }

    @Test
    fun answersToUnknownQuestionsComeLast() {
        val answers = mapOf(
            QuestionId("q-zzz") to Answer.Rating(1),
            QuestionId("q-aaa") to Answer.Rating(2),
            NAP.id to Answer.YesNo(true),
        )
        val json =
            parse(JsonExport.encode(exportData(checkIns = listOf(checkIn(2, answers))), exportedAt))

        val ids = json.array("checkIns")[0].jsonObject.array("answers")
            .map { it.jsonObject.string("questionId") }

        assertEquals(listOf("q-nap", "q-aaa", "q-zzz"), ids)
    }

    @Test
    fun textIsEscaped() {
        val text = "Line 1\nLine 2 with \"quotes\", a tab\t and a backslash \\ é 😴"
        val data = exportData(
            checkIns = listOf(checkIn(2, mapOf(REMARKS.id to Answer.FreeText(text)))),
        )

        val answer = parse(JsonExport.encode(data, exportedAt))
            .array("checkIns")[0].jsonObject.array("answers")[0].jsonObject

        assertEquals(text, answer.string("text"))
    }

    @Test
    fun emptyExportHasEveryList() {
        val empty = ExportData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

        val json = parse(JsonExport.encode(empty, exportedAt))

        assertEquals(
            listOf(
                "format",
                "version",
                "exportedAt",
                "projects",
                "hypotheses",
                "experiments",
                "checkIns",
                "notes",
            ),
            json.keys.toList(),
        )
        assertEquals(JsonPrimitive(1), json["version"])
        for (list in listOf("projects", "hypotheses", "experiments", "checkIns", "notes")) {
            assertEquals(JsonArray(emptyList()), json[list], list)
        }
    }

    private fun parse(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    private fun JsonObject.array(key: String) = getValue(key).jsonArray

    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content

    private fun resource(name: String): String =
        checkNotNull(javaClass.classLoader.getResource(name)) { "Missing test resource $name" }
            .readText()
}
