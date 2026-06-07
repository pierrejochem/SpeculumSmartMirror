package org.speculum.modules.calendar

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant

/** A configured calendar source, like one entry in calendar's `calendars: [{url, symbol}]`. */
data class Calendar(val url: String, val symbol: String = "calendar")

/** A parsed VEVENT, mirroring the calendar module's event shape. */
data class CalendarEvent(
    val title: String,
    val start: Instant,
    val allDay: Boolean,
    val symbol: String,
)

/**
 * Fetches and parses iCalendar (ICS) feeds, like the node_helper behind
 * MagicMirror's `calendar` module. Lightweight line parser keeps it
 * multiplatform (no ICS library dependency). Recurring (RRULE) events are
 * not expanded; single-instance VEVENTs cover holiday-style feeds.
 */
class CalendarProvider {

    private val client by lazy { HttpClient() }
    private val tz = TimeZone.currentSystemDefault()

    suspend fun fetch(calendars: List<Calendar>): List<CalendarEvent> =
        calendars.flatMap { cal ->
            runCatching {
                parse(client.get(cal.url) {
                    header("User-Agent", "Speculum/1.0")
                }.bodyAsText(), cal.symbol)
            }.getOrDefault(emptyList())
        }

    internal fun parse(ics: String, symbol: String): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        var inEvent = false
        var summary: String? = null
        var start: Instant? = null
        var allDay = false

        for (line in unfold(ics)) {
            when {
                line == "BEGIN:VEVENT" -> { inEvent = true; summary = null; start = null; allDay = false }
                line == "END:VEVENT" -> {
                    if (inEvent && summary != null && start != null) {
                        events += CalendarEvent(summary!!, start!!, allDay, symbol)
                    }
                    inEvent = false
                }
                !inEvent -> {}
                line.startsWith("SUMMARY") -> summary = unescape(line.substringAfter(':', ""))
                line.startsWith("DTSTART") -> {
                    val prop = line.substringBefore(':')
                    val value = line.substringAfter(':', "")
                    allDay = prop.contains("VALUE=DATE", ignoreCase = true) || value.length == 8
                    start = parseDate(value, allDay)
                }
            }
        }
        return events
    }

    /** Joins ICS folded lines (continuations begin with a space or tab). */
    private fun unfold(ics: String): List<String> {
        val out = mutableListOf<String>()
        for (raw in ics.split("\n")) {
            val line = raw.trimEnd('\r')
            if ((line.startsWith(" ") || line.startsWith("\t")) && out.isNotEmpty()) {
                out[out.lastIndex] = out.last() + line.substring(1)
            } else out += line
        }
        return out
    }

    /** "20260704" (all-day) or "20260704T130000Z" / "20260704T130000" -> Instant. */
    private fun parseDate(value: String, allDay: Boolean): Instant? = runCatching {
        if (allDay) {
            val y = value.substring(0, 4).toInt()
            val m = value.substring(4, 6).toInt()
            val d = value.substring(6, 8).toInt()
            LocalDate(y, m, d).atStartOfDayIn(tz)
        } else {
            val y = value.substring(0, 4).toInt()
            val m = value.substring(4, 6).toInt()
            val d = value.substring(6, 8).toInt()
            val hh = value.substring(9, 11).toInt()
            val mm = value.substring(11, 13).toInt()
            val ss = value.substring(13, 15).toInt()
            val ldt = LocalDateTime(y, m, d, hh, mm, ss)
            if (value.endsWith("Z")) ldt.toInstant(TimeZone.UTC) else ldt.toInstant(tz)
        }
    }.getOrNull()

    private fun unescape(s: String): String =
        s.replace("\\,", ",").replace("\\;", ";")
            .replace("\\n", " ").replace("\\N", " ").replace("\\\\", "\\").trim()
}