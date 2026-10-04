package org.speculum.modules.weather

import org.speculum.config.ModuleConfig
import org.speculum.config.SettingSpec
import org.speculum.config.SettingType
import org.speculum.core.MirrorModule
import org.speculum.core.ModuleFactory

class WeatherModuleFactory : ModuleFactory {
    override val name = "weather"
    override val order = 0
    override fun settingsSchema() = listOf(
        SettingSpec(
            key = "location", label = "Location name", default = "Hamburg",
            help = "Label shown above the reading; does not affect the forecast.",
        ),
        SettingSpec(
            key = "lat", label = "Latitude", default = "53.55",
            placeholder = "53.55", help = "Decimal degrees, north positive.",
        ),
        SettingSpec(
            key = "lon", label = "Longitude", default = "9.99",
            placeholder = "9.99", help = "Decimal degrees, east positive.",
        ),
        SettingSpec(
            key = "units", label = "Units", type = SettingType.ENUM,
            default = "metric", options = listOf("metric", "imperial"),
        ),
    )

    override fun create(config: ModuleConfig): MirrorModule = WeatherModule(config)
    override fun defaultConfig() = ModuleConfig(
        module = "weather",
        position = "top_right",
        refreshIntervalMs = 600_000,
        config = mapOf(
            "location" to "Hamburg",
            "lat" to "53.55",
            "lon" to "9.99",
            "units" to "metric",
        )
    )
}

class WeatherForecastModuleFactory : ModuleFactory {
    override val name = "weatherforecast"
    override val order = 1
    override fun settingsSchema() = listOf(
        SettingSpec(
            key = "lat", label = "Latitude", default = "53.55",
            placeholder = "53.55", help = "Decimal degrees, north positive.",
        ),
        SettingSpec(
            key = "lon", label = "Longitude", default = "9.99",
            placeholder = "9.99", help = "Decimal degrees, east positive.",
        ),
        SettingSpec(
            key = "units", label = "Units", type = SettingType.ENUM,
            default = "metric", options = listOf("metric", "imperial"),
        ),
        SettingSpec(
            key = "maxNumberOfDays", label = "Forecast days", type = SettingType.INT,
            default = "5", min = 1, max = 7,
        ),
    )

    override fun create(config: ModuleConfig): MirrorModule = WeatherForecastModule(config)
    override fun defaultConfig() = ModuleConfig(
        module = "weatherforecast",
        position = "top_right",
        refreshIntervalMs = 600_000,
        config = mapOf(
            "lat" to "53.55",
            "lon" to "9.99",
            "units" to "metric",
            "maxNumberOfDays" to "5",
        )
    )
}