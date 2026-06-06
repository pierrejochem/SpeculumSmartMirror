package org.speculum.modules.weather

import org.speculum.config.ModuleConfig
import org.speculum.core.MirrorModule
import org.speculum.core.ModuleFactory

class WeatherModuleFactory : ModuleFactory {
    override val name = "weather"
    override val order = 0
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