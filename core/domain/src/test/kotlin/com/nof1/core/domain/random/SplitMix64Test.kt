package com.nof1.core.domain.random

import kotlin.test.Test
import kotlin.test.assertEquals

/** Expected values come from an independent Python implementation of SplitMix64. */
class SplitMix64Test {
    @Test
    fun `matches the reference output for seed 0`() {
        val random = SplitMix64(0)

        assertEquals(0xE220A8397B1DCDAFuL.toLong(), random.nextLong())
        assertEquals(0x6E789E6AA1B965F4uL.toLong(), random.nextLong())
        assertEquals(0x06C45D188009454FuL.toLong(), random.nextLong())
    }

    @Test
    fun `matches the reference output for a negative seed`() {
        val random = SplitMix64(-1)

        assertEquals(0xE4D971771B652C20uL.toLong(), random.nextLong())
        assertEquals(0xE99FF867DBF682C9uL.toLong(), random.nextLong())
        assertEquals(0x382FF84CB27281E9uL.toLong(), random.nextLong())
    }
}
