package org.speculum.modules.weather

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

/** Current conditions, mirroring MagicMirror's `weather` (current) module. */
data class WeatherData(
    val temperature: Int,
    val feelsLike: Int,
    val windSpeed: Int,
    val icon: String,
    val code: Int,
    val sunrise: String,
    val sunset: String,
)

/** One day in the weekly outlook, mirroring MagicMirror's `weatherforecast`. */
data class DailyForecast(
    val day: String,
    val icon: String,
    val code: Int,
    val max: Int,
    val min: Int,
)

/**
 * Provider abstraction mirrors MagicMirror's weatherprovider system, letting
 * you swap data sources. This default uses the free Open-Meteo API (no key).
 */
interface WeatherProvider {
    suspend fun fetch(): WeatherData
    suspend fun fetchForecast(days: Int): List<DailyForecast>
}

class OpenMeteoProvider(
    private val latitude: Double,
    private val longitude: Double,
    private val metric: Boolean
) : WeatherProvider {

    private val client = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private suspend fun query(): OpenMeteoResponse {
        val tempUnit = if (metric) "celsius" else "fahrenheit"
        val windUnit = if (metric) "kmh" else "mph"
        return client.get(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset" +
                "&temperature_unit=$tempUnit&wind_speed_unit=$windUnit&timezone=auto"
        ).body()
    }

    override suspend fun fetch(): WeatherData {
        val r = query()
        val c = r.current
        return WeatherData(
            temperature = c.temperature.roundToInt(),
            feelsLike = c.apparentTemperature.roundToInt(),
            windSpeed = c.windSpeed.roundToInt(),
            icon = iconFor(c.weatherCode),
            code = c.weatherCode,
            sunrise = hhmm(r.daily.sunrise.firstOrNull()),
            sunset = hhmm(r.daily.sunset.firstOrNull()),
        )
    }

    override suspend fun fetchForecast(days: Int): List<DailyForecast> {
        val d = query().daily
        return d.time.indices.take(days).map { i ->
            DailyForecast(
                day = weekday(d.time[i]),
                icon = iconFor(d.weatherCode[i]),
                code = d.weatherCode[i],
                max = d.tempMax[i].roundToInt(),
                min = d.tempMin[i].roundToInt(),
            )
        }
    }

    /** "2026-06-05T05:12" -> "05:12" */
    private fun hhmm(iso: String?): String =
        iso?.substringAfter('T', "")?.take(5).orEmpty()

    /** "2026-06-05" -> "Fri" */
    private fun weekday(date: String): String =
        runCatching {
            LocalDate.parse(date).dayOfWeek.name.lowercase()
                .replaceFirstChar { it.uppercase() }.take(3)
        }.getOrDefault(date)

    // Monochrome text glyphs (+ U+FE0E forces text, not color emoji) so they
    // tint white for a high-contrast two-way mirror.
    private fun iconFor(code: Int): String = when (code) {
        0 -> "☀"; in 1..3 -> "☁"; 45, 48 -> "▒"
        in 51..67 -> "☂"; in 71..77 -> "❄"; in 80..82 -> "☂"
        in 95..99 -> "☇"; else -> "☁"
    } + "︎"
}

@Serializable private data class OpenMeteoResponse(val current: Current, val daily: Daily)

@Serializable private data class Current(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("weather_code") val weatherCode: Int
)

@Serializable private data class Daily(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("temperature_2m_max") val tempMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val tempMin: List<Double> = emptyList(),
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList()
)