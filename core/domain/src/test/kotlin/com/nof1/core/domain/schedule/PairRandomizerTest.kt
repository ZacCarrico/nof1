package com.nof1.core.domain.schedule

import com.nof1.core.domain.AB
import com.nof1.core.domain.BA
import com.nof1.core.domain.date
import com.nof1.core.domain.design
import com.nof1.core.model.PairOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PairRandomizerTest {
    @Test
    fun `orders are reproducible from the seed`() {
        // From an independent Python implementation: the sign bit of each SplitMix64 output.
        assertEquals(
            listOf(BA, AB, AB, AB, AB, BA, AB, BA),
            PairRandomizer.pairOrders(seed = 42, pairCount = 8),
        )
    }

    @Test
    fun `schedule keeps the seed and uses the design's pair count`() {
        val schedule = PairRandomizer.schedule(design(pairCount = 5), date(11, 2), seed = 42)

        assertEquals(date(11, 2), schedule.startDate)
        assertEquals(42L, schedule.seed)
        assertEquals(listOf(BA, AB, AB, AB, AB), schedule.pairOrders)
    }

    @Test
    fun `consistency check catches an edited order`() {
        val schedule = PairRandomizer.schedule(design(pairCount = 5), date(11, 2), seed = 42)

        assertTrue(PairRandomizer.isConsistent(schedule))
        assertFalse(PairRandomizer.isConsistent(schedule.copy(pairOrders = List(5) { AB })))
    }

    @Test
    fun `both orders are about equally likely`() {
        val orders = (0L until 1_000L).flatMap { seed ->
            PairRandomizer.pairOrders(seed, pairCount = 10)
        }
        val interventionFirstShare =
            orders.count { it == PairOrder.INTERVENTION_FIRST } / orders.size.toDouble()

        assertTrue(interventionFirstShare in 0.47..0.53, "share was $interventionFirstShare")
    }
}
