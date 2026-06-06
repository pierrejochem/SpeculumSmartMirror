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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.speculum.config.ModuleConfig
import org.speculum.core.MirrorModule

/**
 * Port of MagicMirror's `weather` module (current conditions). Fetches via a
 * pluggable provider, refreshed on the module's interval, and shows the big
 * temperature plus wind, feels-like, and sunrise/sunset like the demo mirror.
 */
class WeatherModule(config: ModuleConfig) : MirrorModule(config) {

    private val metric = config.string("units", "metric") == "metric"
    private val provider = OpenMeteoProvider(
        latitude = config.string("lat", "53.55").toDouble(),
        longitude = config.string("lon", "9.99").toDouble(),
        metric = metric
    )
    private val windUnit = if (metric) "km/h" else "mph"

    override val refreshIntervalMs: Long = config.refreshIntervalMs.coerceAtLeast(600_000)

    private var state by mutableStateOf<WeatherData?>(null)
    private var error by mutableStateOf<String?>(null)

    override suspend fun refresh() {
        runCatching { provider.fetch() }
            .onSuccess { state = it; error = null }
            .onFailure { error = "Weather unavailable" }
    }

    @Composable
    override fun Content() {
        Column(horizontalAlignment = Alignment.End) {
            Text(config.string("location", "Hamburg"), color = DIM, fontSize = 16.sp)
            when {
                error != null -> Text(error!!, color = DIM, fontSize = 20.sp)
                state == null -> Text("Loading…", color = DIM, fontSize = 20.sp)
                else -> {
                    val w = state!!
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("≈︎ ${w.windSpeed} $windUnit", color = DIM, fontSize = 16.sp)
                        Spacer(Modifier.width(12.dp))
                        Text("↑︎ ${w.sunrise}  ↓︎ ${w.sunset}", color = DIM, fontSize = 16.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        WeatherGlyph(w.code, size = 52.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("${w.temperature}°", color = Color.White, fontSize = 56.sp,
                            fontWeight = FontWeight.Light)
                    }
                    Text("Feels like ${w.feelsLike}°", color = DIM, fontSize = 16.sp,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }

    private companion object { val DIM = Color(0xFF999999) }
}