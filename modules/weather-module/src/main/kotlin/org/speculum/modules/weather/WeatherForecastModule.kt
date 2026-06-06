package org.speculum.modules.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.speculum.config.ModuleConfig
import org.speculum.core.MirrorModule

/**
 * Port of MagicMirror's `weatherforecast` module: a multi-day outlook table
 * with day name, icon, and high/low temperatures.
 */
class WeatherForecastModule(config: ModuleConfig) : MirrorModule(config) {

    private val metric = config.string("units", "metric") == "metric"
    private val provider = OpenMeteoProvider(
        latitude = config.string("lat", "53.55").toDouble(),
        longitude = config.string("lon", "9.99").toDouble(),
        metric = metric
    )
    private val days = config.int("maxNumberOfDays", 5).coerceIn(1, 7)

    override val refreshIntervalMs: Long = config.refreshIntervalMs.coerceAtLeast(600_000)

    private var forecast by mutableStateOf<List<DailyForecast>>(emptyList())

    override suspend fun refresh() {
        runCatching { provider.fetchForecast(days) }.onSuccess { forecast = it }
    }

    @Composable
    override fun Content() {
        Column {
            forecast.forEach { d ->
                Row(
                    Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(d.day, color = DIM, fontSize = 18.sp, modifier = Modifier.width(48.dp))
                    WeatherGlyph(d.code, size = 24.dp, modifier = Modifier.width(36.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${d.max}°", color = Color.White, fontSize = 18.sp,
                        textAlign = TextAlign.End, modifier = Modifier.width(40.dp))
                    Text("${d.min}°", color = DIM, fontSize = 18.sp,
                        textAlign = TextAlign.End, modifier = Modifier.width(40.dp))
                }
            }
        }
    }

    private companion object { val DIM = Color(0xFF999999) }
}