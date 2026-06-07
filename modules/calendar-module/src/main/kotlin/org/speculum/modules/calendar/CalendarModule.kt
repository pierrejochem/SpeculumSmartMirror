package org.speculum.modules.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.speculum.config.ModuleConfig
import org.speculum.core.MirrorModule

/**
 * Port of MagicMirror's `calendar` module. Fetches ICS feeds, sorts the
 * upcoming events, and lists them with a symbol, title, and relative time.
 *
 * Config keys (mirroring calendar's options):
 *  - url / symbol           : the default single calendar (defaults to US Holidays)
 *  - header                 : optional heading above the list
 *  - maximumEntries         : max events shown (default 10)
 *  - maximumNumberOfDays    : only events within this many days (default 365)
 */
class CalendarModule(config: ModuleConfig) : MirrorModule(config) {

    private val calendars = listOf(
        Calendar(
            url = config.string(
                "url",
                "https://ics.calendarlabs.com/76/mm3137/US_Holidays.ics"
            ),
            symbol = config.string("symbol", "calendar-check")
        )
    )
    private val header = config.string("header", "US Holidays")
    private val maxEntries = config.int("maximumEntries", 10).coerceAtLeast(1)
    private val maxDays = config.int("maximumNumberOfDays", 365).coerceAtLeast(1)

    private val tz = TimeZone.currentSystemDefault()
    private val provider = CalendarProvider()
    private var events by mutableStateOf<List<CalendarEvent>>(emptyList())

    override val refreshIntervalMs: Long = config.refreshIntervalMs.coerceAtLeast(300_000)

    override suspend fun refresh() {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(tz).date
        val cutoff = today.plus(maxDays, DateTimeUnit.DAY)
        provider.fetch(calendars)
            .filter { it.start >= now || it.start.toLocalDateTime(tz).date == today }
            .filter { it.start.toLocalDateTime(tz).date < cutoff }
            .sortedBy { it.start }
            .take(maxEntries)
            .let { events = it }
    }

    @Composable
    override fun Content() {
        Column(Modifier.width(340.dp)) {
            if (header.isNotBlank()) Text(
                header.uppercase(), color = FAINT, fontSize = 14.sp,
                letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 4.dp)
            )
            if (events.isEmpty()) {
                Text("No events", color = FAINT, fontSize = 16.sp)
            } else events.forEach { e ->
                Row(
                    Modifier.padding(top = 4.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val glyph = symbolGlyph(e.symbol)
                    if (glyph == null) {
                        CalendarGlyph(size = 15.dp, modifier = Modifier.width(24.dp))
                    } else {
                        Text(glyph, color = Color.White, fontSize = 16.sp,
                            modifier = Modifier.width(24.dp))
                    }
                    Text(
                        e.title, color = Color.White, fontSize = 16.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Text(
                        relative(e), color = DIM, fontSize = 16.sp,
                        maxLines = 1, softWrap = false
                    )
                }
            }
        }
    }

    private fun relative(e: CalendarEvent): String {
        val today = Clock.System.now().toLocalDateTime(tz).date
        val date = e.start.toLocalDateTime(tz).date
        val days = today.daysUntil(date)
        val day = when {
            days == 0 -> "Today"
            days == 1 -> "Tomorrow"
            days in 2..6 -> date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            else -> {
                val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
                "$month ${date.dayOfMonth}"
            }
        }
        if (e.allDay) return day
        val t = e.start.toLocalDateTime(tz)
        val hhmm = "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
        return "$day $hhmm"
    }

    /**
     * Text glyph for a symbol name, or null to draw the vector calendar icon
     * (used for "calendar"/"calendar-check"/default).
     */
    private fun symbolGlyph(name: String): String? = when {
        name.contains("birthday") || name.contains("gift") -> "✦︎"
        name.contains("clock") || name.contains("time") -> "◷︎"
        else -> null
    }

    private companion object {
        val DIM = Color(0xFF999999)
        val FAINT = Color(0xFF666666)
    }
}