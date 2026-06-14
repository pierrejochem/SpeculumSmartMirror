package org.speculum.modules.news

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.speculum.config.ModuleConfig
import org.speculum.core.MirrorModule
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Port of MagicMirror's `newsfeed` module: fetches RSS/Atom feeds, rotates
 * through the headlines, and shows the source title and publish age.
 *
 * Config keys (mirroring newsfeed's options):
 *  - title / url      : the default single feed (defaults to the New York Times)
 *  - updateInterval   : seconds between headline rotations (alias: rotateInterval)
 *  - reloadInterval   : seconds between feed refetches (via refreshInterval)
 *  - showSourceTitle  : show the feed name above the headline (default true)
 *  - showPublishDate  : show "x ago" next to the source (default true)
 */
class NewsFeedModule(config: ModuleConfig) : MirrorModule(config) {

    private val feeds = listOf(
        Feed(
            title = config.string("title", "New York Times"),
            url = config.string("url", "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml")
        )
    )
    private val rotateMs =
        config.int("updateInterval", config.int("rotateInterval", 10)).coerceAtLeast(3) * 1000L
    private val showSourceTitle = config.bool("showSourceTitle", true)
    private val showPublishDate = config.bool("showPublishDate", true)

    private val provider = NewsProvider()
    private var items by mutableStateOf<List<NewsItem>>(emptyList())
    private var index by mutableStateOf(0)

    override val refreshIntervalMs: Long = config.refreshIntervalMs.coerceAtLeast(300_000)

    override fun start(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                delay(rotateMs.milliseconds)
                if (items.isNotEmpty()) index = (index + 1) % items.size
            }
        }
    }

    override suspend fun refresh() {
        val fetched = provider.fetch(feeds)
        if (fetched.isNotEmpty()) {
            items = fetched
            if (index >= fetched.size) index = 0
        }
    }

    @Composable
    override fun Content() {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val item = items.getOrNull(index)
            if (item == null) {
                Text("Loading…", color = DIM, fontSize = 18.sp)
            } else {
                if (showSourceTitle) {
                    val age = if (showPublishDate) relativeTime(item.pubDate) else ""
                    val header = listOf(item.sourceTitle, age).filter { it.isNotBlank() }
                        .joinToString(", ")
                    if (header.isNotBlank()) Text(header, color = DIM, fontSize = 16.sp)
                }
                Box(
                    //modifier = Modifier.fillMaxSize(), // Fives available desktop space
                    contentAlignment = Alignment.Center // Centers the Box content itself
                ) {
                    Text(
                        item.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Light,
                        maxLines = 2,
                        minLines = 2,
                        textAlign = TextAlign.Center, // Centers multiline text within Text bounds
                        modifier = Modifier.wrapContentSize(Alignment.Center)
                    )
                }
            }
        }
    }

    private companion object { val DIM = Color(0xFF999999) }
}

/** Parses an RSS pubDate (RFC-822) and renders it as MagicMirror-style "x ago". */
private fun relativeTime(pubDate: String): String {
    val instant = parseRfc822(pubDate) ?: return ""
    val seconds = (Clock.System.now() - instant).inWholeSeconds
    if (seconds < 0) return "just now"
    return when {
        seconds < 60 -> "${seconds}s ago"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86_400 -> "${seconds / 3600}h ago"
        else -> "${seconds / 86_400}d ago"
    }
}

/** "Wed, 04 Jun 2026 18:30:00 +0000" / "04 Jun 2026 18:30:00 GMT" -> Instant. */
private fun parseRfc822(raw: String): Instant? = runCatching {
    val months = listOf("jan", "feb", "mar", "apr", "may", "jun",
        "jul", "aug", "sep", "oct", "nov", "dec")
    // Drop an optional leading weekday token ("Wed,").
    val t = raw.trim().substringAfter(',', raw.trim()).trim().split(Regex("\\s+"))
    val day = t[0].toInt()
    val month = months.indexOf(t[1].lowercase().take(3)) + 1
    val year = t[2].toInt()
    val time = t[3].split(":")
    val hh = time[0].toInt()
    val mm = time[1].toInt()
    val ss = time.getOrElse(2) { "0" }.toInt()
    val zone = t.getOrElse(4) { "+0000" }
    val offsetSec = when {
        zone.startsWith("+") || zone.startsWith("-") -> {
            val sign = if (zone[0] == '-') -1 else 1
            val z = zone.drop(1).padStart(4, '0')
            sign * (z.take(2).toInt() * 3600 + z.drop(2).take(2).toInt() * 60)
        }
        else -> 0 // GMT/UTC and named zones treated as UTC
    }
    val utc = LocalDateTime(year, month, day, hh, mm, ss).toInstant(TimeZone.UTC)
    utc.minus(offsetSec, DateTimeUnit.SECOND)
}.getOrNull()