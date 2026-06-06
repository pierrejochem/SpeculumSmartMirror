package org.speculum.modules.calendar

import org.speculum.config.ModuleConfig
import org.speculum.core.MirrorModule
import org.speculum.core.ModuleFactory

class CalendarModuleFactory : ModuleFactory {
    override val name = "calendar"
    override val order = 1  // below the clock in top_left
    override fun create(config: ModuleConfig): MirrorModule = CalendarModule(config)
    override fun defaultConfig() = ModuleConfig(
        module = "calendar",
        position = "top_left",
        refreshIntervalMs = 600_000,
        config = mapOf(
            "header" to "US Holidays",
            "symbol" to "calendar-check",
            "url" to "https://ics.calendarlabs.com/76/mm3137/US_Holidays.ics",
            "maximumEntries" to "6",
            "maximumNumberOfDays" to "365",
        )
    )
}