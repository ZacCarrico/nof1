package com.nof1.core.domain.schedule

import com.nof1.core.domain.random.SplitMix64
import com.nof1.core.model.ExperimentDesign
import com.nof1.core.model.PairOrder
import com.nof1.core.model.Schedule
import java.security.SecureRandom
import java.time.LocalDate

/** Chooses the random order of an experiment's pairs when it starts. */
object PairRandomizer {
    /** A seed from a cryptographically strong source, so the order can't be predicted or picked. */
    fun newSeed(): Long = SecureRandom().nextLong()

    /** The pair orders [seed] produces. The same seed always gives the same orders. */
    fun pairOrders(seed: Long, pairCount: Int): List<PairOrder> {
        require(pairCount > 0) { "pairCount must be positive, was $pairCount" }
        val random = SplitMix64(seed)
        return List(pairCount) {
            if (random.nextBoolean()) PairOrder.INTERVENTION_FIRST else PairOrder.BASELINE_FIRST
        }
    }

    fun schedule(design: ExperimentDesign, startDate: LocalDate, seed: Long = newSeed()): Schedule =
        Schedule(startDate, pairOrders(seed, design.pairCount), seed)

    /** Whether [schedule]'s pair orders are the ones its seed produces. */
    fun isConsistent(schedule: Schedule): Boolean =
        schedule.pairOrders == pairOrders(schedule.seed, schedule.pairOrders.size)
}
