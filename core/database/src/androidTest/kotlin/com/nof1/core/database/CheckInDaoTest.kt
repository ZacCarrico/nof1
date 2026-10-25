package com.nof1.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nof1.core.database.model.asExternalModel
import com.nof1.core.model.Answer
import com.nof1.core.model.QuestionId
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CheckInDaoTest {
    private lateinit var db: Nof1Database

    @Before
    fun createDatabase() {
        db = inMemoryDatabase()
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    private suspend fun checkIns() =
        db.checkInDao().observeForExperiment("experiment").first().map { it.asExternalModel() }

    @Test
    fun savingAgainReplacesTheAnswers() = runTest {
        db.insertProjectAndHypothesis()
        db.save(experiment())
        val original = checkIn()
        db.save(original)

        val edited = original.copy(
            followedPlan = false,
            answers = mapOf(sleepQuestion().id to Answer.Rating(9)),
            updatedAt = UPDATED.plusSeconds(60),
        )
        db.save(edited)

        assertEquals(listOf(edited), checkIns())
        assertEquals(1L, db.rowCount("answers"))
    }

    @Test
    fun aSecondCheckInForTheSameDayIsRejected() = runTest {
        db.insertProjectAndHypothesis()
        db.save(experiment())
        db.save(checkIn(id = "first"))

        assertFailsWith<IllegalArgumentException> { db.save(checkIn(id = "second")) }

        assertEquals(listOf("first"), checkIns().map { it.id.value })
    }

    @Test
    fun anAnswerToAnUnknownQuestionIsRejected() = runTest {
        db.insertProjectAndHypothesis()
        db.save(experiment())

        assertFails {
            db.save(checkIn(answers = mapOf(QuestionId("unknown") to Answer.YesNo(true))))
        }

        assertEquals(emptyList(), checkIns())
    }

    @Test
    fun checkInsAreListedByStudyDay() = runTest {
        db.insertProjectAndHypothesis()
        db.save(experiment())
        db.save(checkIn(id = "later", studyDay = LocalDate.of(2026, 3, 4)))
        db.save(checkIn(id = "earlier", studyDay = LocalDate.of(2026, 3, 3)))

        assertEquals(listOf("earlier", "later"), checkIns().map { it.id.value })
    }
}
