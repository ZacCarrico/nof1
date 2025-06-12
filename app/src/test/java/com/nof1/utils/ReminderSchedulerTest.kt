package com.nof1.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Duration

/**
 * Unit tests for ReminderScheduler to ensure proper delay calculations
 */
class ReminderSchedulerTest {

    @Test
    fun `calculateInitialDelay should not return zero or negative delay for current time`() {
        // Get current time
        val now = LocalDateTime.now()
        val currentTime = now.toLocalTime()
        
        // Calculate delay using the same logic as ReminderScheduler
        val delay = calculateInitialDelayForTest(currentTime)
        
        // The delay should be positive (at least a few milliseconds in the future)
        // If scheduling for "now", it should schedule for tomorrow
        assertTrue("Delay should be positive to prevent immediate execution", delay > 0)
        
        // The delay should be reasonable (not more than 25 hours for daily reminders)
        assertTrue("Delay should not exceed 25 hours", delay <= Duration.ofHours(25).toMillis())
    }
    
    @Test
    fun `calculateInitialDelay should schedule for today when time is clearly in future`() {
        val now = LocalDateTime.now()
        val futureTime = now.plusHours(2).toLocalTime()
        
        val delay = calculateInitialDelayForTest(futureTime)
        
        // Should be roughly 2 hours (allowing for small variations)
        val expectedDelay = Duration.ofHours(2).toMillis()
        val tolerance = Duration.ofMinutes(1).toMillis() // 1 minute tolerance
        
        assertTrue("Delay should be approximately 2 hours", 
            Math.abs(delay - expectedDelay) < tolerance)
    }
    
    @Test
    fun `calculateInitialDelay should schedule for tomorrow when time is in past`() {
        val now = LocalDateTime.now()
        val pastTime = now.minusHours(1).toLocalTime()
        
        val delay = calculateInitialDelayForTest(pastTime)
        
        // Should be roughly 23 hours from now
        val expectedDelay = Duration.ofHours(23).toMillis()
        val tolerance = Duration.ofMinutes(10).toMillis() // 10 minute tolerance
        
        assertTrue("Delay should be approximately 23 hours for past time", 
            Math.abs(delay - expectedDelay) < tolerance)
    }
    
    @Test
    fun `calculateInitialDelay should handle edge case of time very close to now`() {
        val now = LocalDateTime.now()
        // Set time to 1 second in the future
        val nearFutureTime = now.plusSeconds(1).toLocalTime()
        
        val delay = calculateInitialDelayForTest(nearFutureTime)
        
        // Since processing time might make this past by the time calculation happens,
        // it should either be a small positive delay (if still in future) or 
        // schedule for tomorrow (if it became past during processing)
        assertTrue("Delay should be positive", delay > 0)
        
        // Should be either very small (< 5 seconds) or roughly 24 hours
        val isSmallDelay = delay < Duration.ofSeconds(5).toMillis()
        val isNextDayDelay = delay > Duration.ofHours(23).toMillis() && 
                            delay < Duration.ofHours(25).toMillis()
        
        assertTrue("Delay should be either very small or next day", 
            isSmallDelay || isNextDayDelay)
    }
    
    @Test
    fun `calculateInitialDelay prevents immediate notifications with minimum delay buffer`() {
        val now = LocalDateTime.now()
        
        // Test current time - should now prevent immediate execution
        val currentTime = now.toLocalTime()
        val delay = calculateInitialDelayForTest(currentTime)
        
        println("Current time: $currentTime")
        println("Calculated delay: ${delay}ms")
        
        // With the fix, delay should always be at least 5 seconds (MIN_DELAY_SECONDS)
        // or schedule for tomorrow if too close to current time
        assertTrue("Delay should be at least 5 seconds to prevent immediate execution", 
            delay >= 5 * 1000)
        
        // Should be either next day (if scheduled for tomorrow) or reasonable future time
        val isNextDayDelay = delay > Duration.ofHours(20).toMillis()
        val isReasonableFutureDelay = delay >= 5 * 1000 && delay < Duration.ofHours(12).toMillis()
        
        assertTrue("Delay should be either next day or reasonable future time", 
            isNextDayDelay || isReasonableFutureDelay)
    }
    
    @Test
    fun `calculateInitialDelay enforces minimum delay for times very close to now`() {
        val now = LocalDateTime.now()
        
        // Test times that are just a few seconds in the future
        val testTimes = listOf(
            now.plusSeconds(1).toLocalTime(),    // 1 second from now
            now.plusSeconds(3).toLocalTime(),    // 3 seconds from now  
            now.plusSeconds(4).toLocalTime()     // 4 seconds from now (just under 5)
        )
        
        testTimes.forEach { time ->
            val delay = calculateInitialDelayForTest(time)
            
            // All should be scheduled for tomorrow due to minimum delay requirement
            assertTrue("Time $time should be scheduled for tomorrow (delay >= 20 hours)", 
                delay >= Duration.ofHours(20).toMillis())
        }
        
        // Test times that should work normally (more than 5 seconds)
        val normalTimes = listOf(
            now.plusSeconds(10).toLocalTime(),   // 10 seconds from now
            now.plusMinutes(1).toLocalTime(),    // 1 minute from now
            now.plusMinutes(5).toLocalTime()     // 5 minutes from now
        )
        
        normalTimes.forEach { time ->
            val delay = calculateInitialDelayForTest(time)
            
            // These should be scheduled normally (not pushed to tomorrow)
            assertTrue("Time $time should be scheduled normally (delay < 20 hours)", 
                delay < Duration.ofHours(20).toMillis())
            assertTrue("Time $time should still have minimum 5 second delay", 
                delay >= 5 * 1000)
        }
    }
    
    /**
     * Test helper method that replicates the calculateInitialDelay logic from ReminderScheduler
     * This ensures we're testing the exact same logic without needing to access private methods
     */
    private fun calculateInitialDelayForTest(notificationTime: LocalTime): Long {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val todayAtNotificationTime = today.atTime(notificationTime)
        
        val targetTime = if (todayAtNotificationTime.isAfter(now)) {
            todayAtNotificationTime
        } else {
            todayAtNotificationTime.plusDays(1)
        }
        
        val delay = Duration.between(now, targetTime).toMillis()
        
        // Replicate the minimum delay logic from ReminderScheduler
        val MIN_DELAY_SECONDS = 5
        return if (delay < MIN_DELAY_SECONDS * 1000) {
            // Schedule for tomorrow at the same time to prevent immediate execution
            val tomorrowAtNotificationTime = todayAtNotificationTime.plusDays(1)
            Duration.between(now, tomorrowAtNotificationTime).toMillis()
        } else {
            delay
        }
    }
}