package org.speculum.modules.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import org.speculum.config.ModuleConfig
import org.speculum.core.MirrorModule
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Port of MagicMirror's default `clock` module.
 *
 * Config keys (mirroring clock's options):
 *  - timeFormat       : 12 or 24 (default 24)
 *  - displaySeconds   : show seconds (default true)
 *  - showPeriod       : show AM/PM in 12h mode (default true)
 *  - showPeriodUpper  : uppercase the AM/PM (default false)
 *  - clockBold        : bold the hours (default false)
 *  - showDate         : show the date line (default true)
 *  - showWeek         : show the ISO week number (default false)
 *  - displayType      : "digital" | "analog" | "both" (default "digital")
 *  - analogSize       : analog clock diameter in dp (default 200)
 *  - analogPlacement  : "top" | "bottom" | "left" | "right" (default "bottom")
 */
class ClockModule(config: ModuleConfig) : MirrorModule(config) {

    private val use24h = config.int("timeFormat", 24) == 24
    private val displaySeconds = config.bool("displaySeconds", true)
    private val showPeriod = config.bool("showPeriod", true)
    private val showPeriodUpper = config.bool("showPeriodUpper", false)
    private val clockBold = config.bool("clockBold", false)
    private val showDate = config.bool("showDate", true)
    private val showWeek = config.bool("showWeek", false)
    private val displayType = config.string("displayType", "digital").lowercase()
    private val analogSize = config.int("analogSize", 200).coerceIn(80, 600).dp
    private val analogPlacement = config.string("analogPlacement", "bottom").lowercase()

    private var now by mutableStateOf(Parts(currentDateTime()))

    override fun start(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                now = Parts(currentDateTime())
                delay(1000)
            }
        }
    }

    private fun currentDateTime(): LocalDateTime =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    @Composable
    override fun Content() {
        val showDigital = displayType == "digital" || displayType == "both"
        val showAnalog = displayType == "analog" || displayType == "both"

        when {
            showAnalog && showDigital && (analogPlacement == "left" || analogPlacement == "right") ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (analogPlacement == "left") { AnalogClock(); Spacer(Modifier.width(16.dp)) }
                    Column { Digital() }
                    if (analogPlacement == "right") { Spacer(Modifier.width(16.dp)); AnalogClock() }
                }
            else -> Column(horizontalAlignment = Alignment.Start) {
                if (showAnalog && analogPlacement == "top") {
                    AnalogClock(); Spacer(Modifier.height(12.dp))
                }
                if (showDigital) Digital()
                if (showAnalog && analogPlacement != "top") {
                    Spacer(Modifier.height(12.dp)); AnalogClock()
                }
            }
        }
    }

    @Composable
    private fun Digital() {
        Column {
            if (showDate) Text(now.date, color = DIM, fontSize = 18.sp)
            Text(timeString(), color = Color.White, fontSize = 64.sp,
                fontWeight = if (clockBold) FontWeight.Normal else FontWeight.Thin)
            if (showWeek) Text("Week ${now.week}", color = DIM, fontSize = 16.sp)
        }
    }

    private fun timeString() = buildAnnotatedString {
        val h = if (use24h) now.hour24.toString().padStart(2, '0')
        else (((now.hour24 + 11) % 12) + 1).toString()
        val m = now.minute.toString().padStart(2, '0')

        if (clockBold) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(h) }
        else append(h)
        append(":"); append(m)

        if (displaySeconds) {
            withStyle(SpanStyle(fontSize = 32.sp, color = DIM)) {
                append(":"); append(now.second.toString().padStart(2, '0'))
            }
        }
        if (!use24h && showPeriod) {
            val period = if (now.hour24 < 12) "am" else "pm"
            withStyle(SpanStyle(fontSize = 28.sp)) {
                append(" "); append(if (showPeriodUpper) period.uppercase() else period)
            }
        }
    }

    @Composable
    private fun AnalogClock() {
        Canvas(Modifier.size(analogSize)) {
            val r = min(size.width, size.height) / 2f
            val c = Offset(size.width / 2f, size.height / 2f)

            drawCircle(color = FACE, radius = r, center = c, style = Stroke(width = r * 0.04f))
            for (i in 0 until 12) {
                val a = i * 30.0
                drawLine(FACE, pointOn(c, r * 0.80f, a), pointOn(c, r * 0.92f, a),
                    strokeWidth = r * 0.03f, cap = StrokeCap.Round)
            }
            val hourAngle = ((now.hour24 % 12) + now.minute / 60.0) * 30.0
            val minAngle = (now.minute + now.second / 60.0) * 6.0
            val secAngle = now.second * 6.0
            drawLine(Color.White, c, pointOn(c, r * 0.50f, hourAngle),
                strokeWidth = r * 0.06f, cap = StrokeCap.Round)
            drawLine(Color.White, c, pointOn(c, r * 0.75f, minAngle),
                strokeWidth = r * 0.04f, cap = StrokeCap.Round)
            if (displaySeconds) drawLine(SECONDS, c, pointOn(c, r * 0.85f, secAngle),
                strokeWidth = r * 0.02f, cap = StrokeCap.Round)
            drawCircle(Color.White, radius = r * 0.04f, center = c)
        }
    }

    /** Point at `deg` clockwise from 12 o'clock, `dist` from center. */
    private fun pointOn(c: Offset, dist: Float, deg: Double): Offset {
        val rad = (deg - 90.0) * PI / 180.0
        return Offset(c.x + dist * cos(rad).toFloat(), c.y + dist * sin(rad).toFloat())
    }

    private class Parts(dt: LocalDateTime) {
        val hour24 = dt.hour
        val minute = dt.minute
        val second = dt.second
        val date = "${dt.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, " +
            "${dt.month.name.lowercase().replaceFirstChar { it.uppercase() }} " +
            "${dt.dayOfMonth}, ${dt.year}"
        val week = isoWeek(dt.date)
    }

    private companion object {
        val DIM = Color(0xFF999999)
        val FACE = Color(0xFF666666)
        val SECONDS = Color(0xFFE0533D)
    }
}

/** ISO-8601 week number (weeks start Monday; week 1 contains the first Thursday). */
private fun isoWeek(date: LocalDate): Int {
    val dow = date.dayOfWeek.isoDayNumber // 1=Mon .. 7=Sun
    val week = (date.dayOfYear - dow + 10) / 7
    return when {
        week < 1 -> isoWeek(LocalDate(date.year - 1, 12, 28)) // last week of previous year
        week > 52 -> {
            val dec31Dow = LocalDate(date.year, 12, 31).dayOfWeek.isoDayNumber
            if (week == 53 && dec31Dow < 4) 1 else week
        }
        else -> week
    }
}