package com.axelby.elementarycrm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class TimeUnitTest {

    @Test
    fun testReminderTime() {
        val base = LocalDateTime.of(2018, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC)
        val todayAt7pm = LocalDateTime.of(2018, 1, 1, 19, 0, 0).toInstant(ZoneOffset.UTC)
        assertEquals("today at 7:00 PM", toReminderTime(todayAt7pm, base))
        val tomorrowAt8pm = LocalDateTime.of(2018, 1, 2, 20, 0, 0).toInstant(ZoneOffset.UTC)
        assertEquals("tomorrow at 8:00 PM", toReminderTime(tomorrowAt8pm, base))
        val thursdayAt10am = LocalDateTime.of(2018, 1, 4, 9, 0, 0).toInstant(ZoneOffset.UTC)
        assertEquals("Thursday at 9:00 AM", toReminderTime(thursdayAt10am, base))
        val nextWednesdayAt12pm = LocalDateTime.of(2018, 1, 10, 12, 0, 0).toInstant(ZoneOffset.UTC)
        assertEquals("next Wednesday at 12:00 PM", toReminderTime(nextWednesdayAt12pm, base))
        val jan24At330pm = LocalDateTime.of(2018, 1, 24, 15, 30, 0).toInstant(ZoneOffset.UTC)
        assertEquals("January 24 at 3:30 PM", toReminderTime(jan24At330pm, base))
    }
}
