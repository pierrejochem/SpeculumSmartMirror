package org.speculum.modules.calendar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CalendarProviderTest {

    @Test
    fun parsesAllDayEvent() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Independence Day
            DTSTART;VALUE=DATE:20250704
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()
        val events = CalendarProvider().parse(ics, "calendar")
        assertEquals(1, events.size)
        assertEquals("Independence Day", events[0].title)
        assertTrue(events[0].allDay)
        assertEquals("calendar", events[0].symbol)
    }

    @Test
    fun parsesTimedEventAndUnescapes() {
        val ics = """
            BEGIN:VEVENT
            SUMMARY:Meeting\, important
            DTSTART:20250704T120000Z
            END:VEVENT
        """.trimIndent()
        val events = CalendarProvider().parse(ics, "x")
        assertEquals(1, events.size)
        assertEquals("Meeting, important", events[0].title)  // \, unescaped
        assertFalse(events[0].allDay)
    }

    @Test
    fun ignoresEventsWithoutSummaryOrStart() {
        val ics = "BEGIN:VEVENT\nDTSTART:20250704T120000Z\nEND:VEVENT"
        assertEquals(0, CalendarProvider().parse(ics, "x").size)
    }
}