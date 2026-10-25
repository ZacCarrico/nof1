package com.nof1.core.model

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnswerTypeTest {
    @Test
    fun `rating accepts values on its scale only`() {
        val type = AnswerType.Rating(min = 1, max = 10)

        assertTrue(type.accepts(Answer.Rating(1)))
        assertTrue(type.accepts(Answer.Rating(10)))
        assertFalse(type.accepts(Answer.Rating(0)))
        assertFalse(type.accepts(Answer.Rating(11)))
        assertFalse(type.accepts(Answer.YesNo(true)))
    }

    @Test
    fun `rating needs min below max`() {
        assertFailsWith<IllegalArgumentException> { AnswerType.Rating(min = 5, max = 5) }
    }

    @Test
    fun `number accepts finite values within its optional bounds`() {
        val hoursSlept = AnswerType.NumberWithUnit(unit = "h", decimals = 1, min = 0.0, max = 24.0)

        assertTrue(hoursSlept.accepts(Answer.NumberWithUnit(7.5)))
        assertFalse(hoursSlept.accepts(Answer.NumberWithUnit(-0.5)))
        assertFalse(hoursSlept.accepts(Answer.NumberWithUnit(24.5)))
        assertFalse(hoursSlept.accepts(Answer.NumberWithUnit(Double.NaN)))
        assertTrue(AnswerType.NumberWithUnit(unit = "mg").accepts(Answer.NumberWithUnit(1e6)))
    }

    @Test
    fun `number rejects inverted bounds and too many decimals`() {
        assertFailsWith<IllegalArgumentException> {
            AnswerType.NumberWithUnit(unit = "h", min = 10.0, max = 1.0)
        }
        val tooManyDecimals = AnswerType.NumberWithUnit.MAX_DECIMALS + 1
        assertFailsWith<IllegalArgumentException> {
            AnswerType.NumberWithUnit(unit = "h", decimals = tooManyDecimals)
        }
    }

    @Test
    fun `yes-no and free text accept only their own answers`() {
        assertTrue(AnswerType.YesNo.accepts(Answer.YesNo(false)))
        assertFalse(AnswerType.YesNo.accepts(Answer.FreeText("yes")))
        assertTrue(AnswerType.FreeText.accepts(Answer.FreeText("Slept badly")))
        assertFalse(AnswerType.FreeText.accepts(Answer.Rating(3)))
    }

    @Test
    fun `only free text answers are not quantitative`() {
        assertTrue(AnswerType.Rating().isQuantitative)
        assertTrue(AnswerType.YesNo.isQuantitative)
        assertTrue(AnswerType.NumberWithUnit(unit = "steps").isQuantitative)
        assertFalse(AnswerType.FreeText.isQuantitative)
    }
}
