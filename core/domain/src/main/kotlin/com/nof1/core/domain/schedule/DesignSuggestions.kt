package com.nof1.core.domain.schedule

import com.nof1.core.domain.analysis.Verdicts
import com.nof1.core.model.CheckInDay
import com.nof1.core.model.ExperimentDesign
import java.time.LocalTime

/** How quickly a change takes effect and wears off; decides a sensible period length. */
enum class EffectTiming {
    /** For example caffeine timing or a nap. */
    WITHIN_A_DAY,

    /** For example a bedtime routine. */
    WITHIN_A_FEW_DAYS,

    /** For example a diet change or a supplement. */
    ABOUT_A_WEEK,
}

/**
 * A starting design for the experiment builder. These are suggested starting points rather than
 * established standards, and every value can be changed.
 */
fun suggestedDesign(
    timing: EffectTiming,
    checkInTime: LocalTime,
    checkInDay: CheckInDay,
): ExperimentDesign {
    val (periodLengthDays, settlingInDays, pairCount) = when (timing) {
        EffectTiming.WITHIN_A_DAY -> Triple(1, 0, 10)
        EffectTiming.WITHIN_A_FEW_DAYS -> Triple(3, 1, 6)
        EffectTiming.ABOUT_A_WEEK -> Triple(7, 2, 5)
    }
    return ExperimentDesign(periodLengthDays, settlingInDays, pairCount, checkInTime, checkInDay)
}

enum class DesignWarning {
    /** Even with perfect data, the experiment can't get a verdict, only descriptive results. */
    TOO_FEW_PAIRS_FOR_VERDICT,
}

fun ExperimentDesign.warnings(): Set<DesignWarning> = buildSet {
    if (pairCount < Verdicts.MIN_COMPLETE_PAIRS) add(DesignWarning.TOO_FEW_PAIRS_FOR_VERDICT)
}
