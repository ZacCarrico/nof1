package com.nof1.core.domain.schedule

import com.nof1.core.domain.design
import com.nof1.core.model.CheckInDay
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DesignSuggestionsTest {
    private val nineAm = LocalTime.of(9, 0)

    @Test
    fun `suggestions match how quickly the change acts`() {
        val suggestions = EffectTiming.entries.associateWith {
            suggestedDesign(it, nineAm, CheckInDay.PREVIOUS_DAY)
        }

        assertEquals(
            listOf(Triple(1, 0, 10), Triple(3, 1, 6), Triple(7, 2, 5)),
            suggestions.values.map { Triple(it.periodLengthDays, it.settlingInDays, it.pairCount) },
        )
        assertEquals(listOf(20, 36, 70), suggestions.values.map { it.totalDays })
        suggestions.values.forEach {
            assertEquals(nineAm, it.checkInTime)
            assertEquals(CheckInDay.PREVIOUS_DAY, it.checkInDay)
            assertEquals(emptySet(), it.warnings())
        }
    }

    @Test
    fun `fewer than five pairs can't reach a verdict`() {
        assertEquals(
            setOf(DesignWarning.TOO_FEW_PAIRS_FOR_VERDICT),
            design(pairCount = 4).warnings(),
        )
        assertEquals(emptySet(), design(pairCount = 5).warnings())
    }
}
