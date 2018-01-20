package com.axelby.elementarycrm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class TimeUnitTest {

    @Test
    fun testReminderTime() {
        val base = LocalDateTime.of(2018, 1, 10, 0, 0, 0)
        val lastSaturdayAt9am = LocalDateTime.of(2018, 1, 6, 9, 0, 0)
        assertEquals("last Saturday at 9:00 AM", toReminderTime(lastSaturdayAt9am, base))
        val twoDaysAgoAt7pm = LocalDateTime.of(2018, 1, 8, 19, 0, 0)
        assertEquals("two days ago at 7:00 PM", toReminderTime(twoDaysAgoAt7pm, base))
        val yesterdayAt7pm = LocalDateTime.of(2018, 1, 9, 19, 0, 0)
        assertEquals("yesterday at 7:00 PM", toReminderTime(yesterdayAt7pm, base))
        val todayAt7pm = LocalDateTime.of(2018, 1, 10, 19, 0, 0)
        assertEquals("today at 7:00 PM", toReminderTime(todayAt7pm, base))
        val tomorrowAt8pm = LocalDateTime.of(2018, 1, 11, 20, 0, 0)
        assertEquals("tomorrow at 8:00 PM", toReminderTime(tomorrowAt8pm, base))
        val fridayAt9am = LocalDateTime.of(2018, 1, 12, 9, 0, 0)
        assertEquals("Friday at 9:00 AM", toReminderTime(fridayAt9am, base))
        val nextWednesdayAt12pm = LocalDateTime.of(2018, 1, 17, 12, 0, 0)
        assertEquals("next Wednesday at 12:00 PM", toReminderTime(nextWednesdayAt12pm, base))
        val jan29At330pm = LocalDateTime.of(2018, 1, 29, 15, 30, 0)
        assertEquals("January 29 at 3:30 PM", toReminderTime(jan29At330pm, base))
        val jan1At330pm = LocalDateTime.of(2018, 1, 1, 15, 30, 0)
        assertEquals("January 1 at 3:30 PM", toReminderTime(jan1At330pm, base))
    }
}
